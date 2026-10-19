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
