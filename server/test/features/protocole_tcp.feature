Feature: Protocole TCP du serveur de centralisation

  Background:
    Given un serveur TCP avec de faux drivers

  Scenario: une commande inconnue est rejetée
    When le client envoie "HELLO"
    Then le serveur répond "ERR unknown command"

  Scenario: AUTOREGISTER transmet uc, chipsets et coordonnées à l'API
    Given l'API répond "OK module 3,mod3,abc-123"
    When le client envoie "AUTOREGISTER esp32 lm35 bme280 2.3522 48.8566"
    Then le serveur répond "OK module 3,mod3,abc-123"
    And l'API a reçu uniquement l'appel "autoRegister esp32 [lm35, bme280] 2.3522 48.8566"
    And la base n'a reçu aucun appel

  Scenario Outline: AUTOREGISTER invalide est refusé sans appeler l'API
    When le client envoie "<commande>"
    Then le serveur répond "<erreur>"
    And l'API n'a reçu aucun appel

    Examples:
      | commande                           | erreur                           |
      | AUTOREGISTER esp32 2.35 48.85      | ERR invalid number of parameters |
      | AUTOREGISTER esp32 lm35 abc 48.85  | ERR invalid coordinates          |
      | AUTOREGISTER esp32 lm35 200 48.85  | ERR coordinates out of range     |
      | AUTOREGISTER esp32 lm35 2.35 -91   | ERR coordinates out of range     |

  Scenario: STOREMEASURE passe par l'API
    When le client envoie "STOREMEASURE temperature 2026-11-04T19:51:27 23.1 key-1"
    Then le serveur répond "OK"
    And l'API a reçu uniquement l'appel "saveMeasure temperature 2026-11-04T19:51:27 23.1 key-1"

  Scenario: STOREMEASURE renvoie l'erreur de l'API
    Given l'API répond "ERR invalid module key"
    When le client envoie "STOREMEASURE temperature 2026-11-04T19:51:27 23.1 bad"
    Then le serveur répond "ERR invalid module key"

  Scenario: STOREANALYSIS va directement en base
    When le client envoie "STOREANALYSIS smoke 2026-11-04T19:51:27 1"
    Then le serveur répond "OK"
    And la base a reçu uniquement l'appel "saveAnalysis smoke 2026-11-04T19:51:27 1"
    And l'API n'a reçu aucun appel

  Scenario: GETCONFIG va directement en base
    Given la base répond "OK 60"
    When le client envoie "GETCONFIG key-1"
    Then le serveur répond "OK 60"
    And la base a reçu uniquement l'appel "getConfig key-1"

  Scenario Outline: le nombre de paramètres est vérifié
    When le client envoie "<commande>"
    Then le serveur répond "ERR invalid number of parameters"
    And l'API n'a reçu aucun appel
    And la base n'a reçu aucun appel

    Examples:
      | commande                           |
      | STOREMEASURE temperature 23.1 key-1 |
      | STOREANALYSIS smoke 1              |
      | GETCONFIG                          |
      | GETCONFIG a b                      |

  Scenario: plusieurs requêtes sur la même connexion
    When le client envoie "STOREANALYSIS a 2026-01-01T00:00:00 1"
    Then le serveur répond "OK"
    When le client envoie "STOREANALYSIS b 2026-01-01T00:00:00 2"
    Then le serveur répond "OK"
    And la base a reçu 2 appels

  Scenario: une ligne vide termine le thread
    When le client envoie une ligne vide
    Then le thread du serveur se termine
