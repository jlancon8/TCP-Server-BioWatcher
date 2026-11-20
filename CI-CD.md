# CI/CD et tests du serveur TCP

Deux pipelines équivalents : `.gitlab-ci.yml` (GitLab IUT) et `.github/workflows/ci.yml` (GitHub Actions).
Le code source n'est pas modifié : Maven (`pom.xml`) lit directement `server/src` et `client/src`,
les commandes `javac` du README marchent toujours.

## Tests d'acceptance (Cucumber)

Les tests décrivent le comportement du serveur du point de vue d'un client (le µC) : on ouvre un vrai socket
TCP vers un `ThreadServer`, on envoie des lignes de protocole, on vérifie la réponse. Les scénarios sont écrits
en Gherkin (français), les steps en Java.

```
server/
├── src/                              code de production
└── test/
    ├── features/                     scénarios Gherkin (copiés dans le classpath sous features/)
    │   ├── protocole_tcp.feature     commandes TCP : AUTOREGISTER, STOREMEASURE, STOREANALYSIS, GETCONFIG
    │   ├── driver_http.feature       HttpDataDriver contre une fausse API HTTP embarquée
    │   └── mongo.feature             @mongo : serveur TCP + vrai MongoDB
    ├── ProtocolSteps.java            steps de protocole_tcp.feature
    ├── HttpSteps.java                steps de driver_http.feature
    ├── MongoSteps.java               steps de mongo.feature
    ├── Hooks.java                    @After : ferme sockets, fausse API et connexion Mongo
    ├── MyWorld.java                  état partagé d'un scénario (sockets, fausses API/drivers, client Mongo)
    ├── FakeDataDriver.java           faux driver : enregistre les appels, réponse configurable
    ├── TestExchanger.java            DataExchanger dont les drivers sont remplacés
    └── RunCucumberTest.java          lanceur JUnit : trouve les features et les steps
```

Les classes de test n'ont pas de `package`, comme le code de production qu'elles appellent : elles doivent donc
rester à plat dans `test/` (seul `features/` est un dossier, il ne contient pas de Java).

| Notion | Rôle |
|---|---|
| **Feature** | un fichier `.feature` : une fonctionnalité, ses scénarios |
| **Scenario / Scenario Outline** | un cas de test ; l'Outline rejoue le même scénario avec un tableau d'exemples |
| **Step definition** | méthode Java liée à une phrase (`@Given` contexte, `@When` action, `@Then` vérification) |
| **MyWorld** | objet créé pour chaque scénario et injecté (PicoContainer) dans les classes de steps pour partager l'état |
| **Tag `@mongo`** | scénarios qui ont besoin d'un vrai MongoDB : ignorés (skipped) si `MONGO_URL` n'est pas défini |

Le client (`client/`) n'a pas de tests propres : il est exercé par le smoke test de la CI.

## Étapes du pipeline

| Étape | Ce qui se passe |
|---|---|
| build | `mvn compile` |
| test | `mvn test` : `RunCucumberTest` (surefire) lance tous les scénarios ; sans `MONGO_URL`, les `@mongo` sont skipped |
| package | jar serveur autonome + jar client, téléchargeables dans les artefacts du job |
| integration | `mvn verify` avec un vrai MongoDB et `MONGO_URL` défini : les scénarios `@mongo` s'exécutent ; puis `ci/smoke-test.sh` lance le jar et lui parle en TCP comme un µC |
| integration (e2e-full) | si `API_IMAGE` est défini : MongoDB + API Node + serveur TCP déployés ensemble, chaîne AUTOREGISTER → STOREMEASURE → GETCONFIG |
| docs | Javadoc (`mvn javadoc:aggregate`) |
| deploy | page Pages (téléchargements + Javadoc), image Docker dans le registre, déclenchement des autres composants |
| release | seulement sur un tag `vX.Y.Z` : jars attachés à une release |

Les rapports sont dans `server/target/surefire-reports/`. Failsafe est encore déclaré dans le `pom.xml` mais n'a
plus de tests à lancer (il n'y a plus de classe `*IT`) : tout passe par surefire.

## En local

Sans MongoDB :

```bash
mvn clean test
```

Résultat attendu : les scénarios défilent dans la console (`Given` / `When` / `Then`), puis `BUILD SUCCESS`.
La ligne `Tests run: 22` compte les 22 scénarios. Les 3 scénarios `@mongo` apparaissent en **Skipped** (le chiffre
peut être doublé sur la ligne `-- in RunCucumberTest`, le bon total est celui du bloc `Results`) :
c'est normal, `MONGO_URL` n'est pas défini.

Avec MongoDB (les 3 scénarios `@mongo` s'exécutent, 0 skipped) :

```bash
docker run -d --name mongo-test -p 27099:27017 mongo:latest
```

PowerShell, **dans la même fenêtre** que celle où tu lances Maven :

```powershell
$env:MONGO_URL = "mongodb://localhost:27099"
mvn clean test
```

Bash :

```bash
MONGO_URL=mongodb://localhost:27099 mvn clean test
```

Pour arrêter Mongo ensuite :

```bash
docker stop mongo-test && docker rm mongo-test
```

Notes :
- `-Drevision=...` est facultatif en local : le `pom.xml` a une valeur par défaut (`0.1.0-SNAPSHOT`).
- Si `MONGO_URL` est défini mais que Mongo n'est pas lancé, les scénarios `@mongo` **échouent** (ils ne sont plus skipped).
- Depuis IntelliJ (bouton ▶), la variable se règle dans Run > Edit Configurations > Environment variables.

## Ajouter un test

1. Écrire le scénario dans le `.feature` concerné (ou en créer un dans `server/test/features/`).
2. Lancer `mvn test` : Cucumber affiche les steps non définis avec un squelette Java à copier.
3. Ajouter la méthode dans la classe de steps (`ProtocolSteps`, `HttpSteps` ou `MongoSteps`), l'état partagé va dans `MyWorld`.

## Javadoc

```bash
mvn javadoc:aggregate
```

La documentation HTML est générée dans `target/reports/apidocs/` : ouvre `index.html` dans un navigateur.

## Faire une release

```bash
git tag v1.0.0 && git push origin v1.0.0
```

## Multi-projets

- **GitLab** : variable `INTEGRATION_PROJECT` (ex. `biowatcher/api`) → après un push sur `main`, ce projet est
  déclenché avec `TCP_SERVER_IMAGE`. Dans l'autre sens, un composant peut déclencher celui-ci en passant
  `API_IMAGE` :
  ```yaml
  trigger-tcp:
    stage: deploy
    variables:
      API_IMAGE: $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
    trigger:
      project: biowatcher/<projet-tcp>
      strategy: depend
  ```
- **GitHub** : variable `DOWNSTREAM_REPOS` + secret `COMPONENTS_TOKEN` → `repository_dispatch`
  `component-updated`. Ce dépôt écoute le même événement (payload `api_image`).

## À régler une fois

- GitHub : Settings → Pages → Source = **GitHub Actions**.
- GitLab : le job `docker-image` a besoin d'un runner « privileged » (docker-in-docker) ; il est en
  `allow_failure` si le runner du département ne le permet pas.
- `e2e-full` : les variables passées au conteneur de l'API (`MONGO_URL`, `MONGODB_URI`, `PORT`) sont à adapter
  aux noms réellement lus par l'API.