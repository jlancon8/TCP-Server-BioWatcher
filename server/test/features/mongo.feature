@mongo
Feature: Serveur TCP branché sur un vrai MongoDB

  Background:
    Given une pile TCP branchée sur un vrai MongoDB

  Scenario: GETCONFIG lit l'intervalle d'envoi depuis MongoDB
    Given un module existe en base avec un intervalle d'envoi de 42
    When le client demande la config de ce module
    Then la réponse du serveur commence par "OK"
    And la réponse du serveur contient "42"

  Scenario: GETCONFIG avec une clé inconnue échoue
    When le client demande la config d'une clé inconnue
    Then la réponse du serveur commence par "ERR"

  Scenario: STOREANALYSIS est écrit dans MongoDB
    When le client stocke une analyse unique de valeur "7"
    Then le serveur répond "OK"
    And la mesure est enregistrée en base avec la valeur "7"
