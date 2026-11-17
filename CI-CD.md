# CI/CD du serveur TCP

Deux pipelines équivalents : `.gitlab-ci.yml` (GitLab IUT) et `.github/workflows/ci.yml` (GitHub Actions).
Le code source n'est pas modifié : Maven (`pom.xml`) lit directement `server/src` et `client/src`,
les commandes `javac` du README marchent toujours.

## Étapes

| Étape | Ce qui se passe |
|---|---|
| build | `mvn compile` |
| test | tests unitaires JUnit (`server/test/*Test.java`) : protocole TCP avec de faux drivers, driver HTTP contre une fausse API |
| package | jar serveur autonome + jar client, téléchargeables dans les artefacts du job |
| integration | `*IT.java` contre un vrai MongoDB ; puis `ci/smoke-test.sh` lance le jar et lui parle en TCP comme un µC |
| integration (e2e-full) | si `API_IMAGE` est défini : MongoDB + API Node + serveur TCP déployés ensemble, chaîne AUTOREGISTER → STOREMEASURE → GETCONFIG |
| docs | Javadoc (`mvn javadoc:aggregate`) |
| deploy | page Pages (téléchargements + Javadoc), image Docker dans le registre, déclenchement des autres composants |
| release | seulement sur un tag `vX.Y.Z` : jars attachés à une release |

## En local

```bash
mvn verify                                  # tests unitaires (IT ignorés sans MongoDB)
docker run -d -p 27099:27017 mongo:7
MONGO_URL=mongodb://localhost:27099 mvn verify   # + tests d'intégration
```

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
