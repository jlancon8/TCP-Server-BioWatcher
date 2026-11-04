# Serveur de centralisation BioWatcher

Prérequis : MongoDB et l'API Node doivent déjà tourner.
Les jars Mongo (`mongodb-driver-sync`, `bson`, `mongodb-driver-core`) sont dans `lib/`.

## Compiler

```
javac -cp ".;..\..\lib\*" BioWatcherCentralServer.java
```

## Lancer le serveur

```
java -cp ".;..\..\lib\*" BioWatcherCentralServer 5000
```

`5000` est le port d'écoute du serveur.

## Lancer le client

```
javac BioWatcherClient.java
```

## Puis

```
java BioWatcherClient 127.0.0.1 5000
```

## Exemples d'utilisation

Le client de test simule un microcontrôleur. Chaque commande est envoyée au serveur TCP, qui répond sur une ligne : `OK ...` ou `ERR ...`.

### AUTOREGISTER

Enregistre un nouveau module avec son type de microcontrôleur et la liste des chipsets qu'il embarque.

```
AUTOREGISTER esp32 lm35 bme280
AUTOREGISTER esp8266 lm35
```

- Le premier exemple enregistre un nouveau module avec un µC esp32 et deux chipsets (lm35 et bme280).
- Le second enregistre un module avec un µC esp8266 et un chipset (lm35).
- Réponse : `OK nom,nomCourt,clé`. La clé est générée par le serveur et sert d'identifiant du module pour les autres commandes.

### STOREMEASURE

Enregistre une mesure envoyée par un module. La clé du module est obligatoire, la date est optionnelle (le client la génère si elle est absente).

```
STOREMEASURE <type> <valeur> <clé>
STOREMEASURE <type> <date> <valeur> <clé>
```

```
STOREMEASURE temperature 23.1 58ae1d8f-027b-4061-a4c7-b37c3f8ed54e
STOREMEASURE humidity 2026-11-04T19:51:27 45.2 58ae1d8f-027b-4061-a4c7-b37c3f8ed54e
```

- Le premier enregistre une mesure de température venant du module ayant cette clé, avec la date du moment.
- Le second enregistre une mesure d'humidité avec une date précise (format `yyyy-MM-ddTHH:mm:ss`, sans espace).
- Réponse : `OK`, ou `ERR invalid module key` si la clé n'existe pas.

### STOREANALYSIS

Enregistre un résultat d'analyse envoyé par le serveur d'analyse multimédia. Aucune clé de module n'est nécessaire.

```
STOREANALYSIS voltage 5
```

- Enregistre une mesure de tension venant du serveur d'analyse, sans module associé.
- Réponse : `OK`.

### GETCONFIG

Récupère la configuration d'un module (intervalle d'envoi des mesures).

```
GETCONFIG 58ae1d8f-027b-4061-a4c7-b37c3f8ed54e
```

- Renvoie l'intervalle d'envoi des mesures du module ayant cette clé.
- Réponse : `OK <intervalle en secondes>` (par exemple `OK 60`), ou `ERR` si la clé est inconnue.