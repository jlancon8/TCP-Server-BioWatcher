Feature: Driver HTTP vers l'API Node

  Background:
    Given une fausse API HTTP embarquée

  Scenario: enregistrement automatique d'un module
    Given la fausse API répond avec le JSON :
      """
      {"error":0,"status":201,"data":{"name":"module 4","shortName":"mod4","key":"k-4"}}
      """
    When le driver HTTP enregistre un module "esp32" avec les chipsets "lm35,bme280" en 2.35 48.85
    Then le driver répond "OK module 4,mod4,k-4"
    And la fausse API a reçu la requête "POST /api/modules/register"
    And le corps JSON a le champ "uc" valant "esp32"
    And le corps JSON a le champ "chipsets" avec la liste "lm35,bme280"
    And la position du corps JSON est un Point en 2.35 48.85

  Scenario: une erreur de l'API devient une ligne ERR
    Given la fausse API répond avec le JSON :
      """
      {"error":1,"status":400,"data":"invalid module key"}
      """
    When le driver HTTP sauvegarde la mesure "temperature" "2026-01-01T00:00:00" "21" avec la clé "bad"
    Then le driver répond "ERR invalid module key"

  Scenario: sauvegarde d'une mesure
    When le driver HTTP sauvegarde la mesure "temperature" "2026-01-01T00:00:00" "21.5" avec la clé "k-1"
    Then le driver répond "OK"
    And la fausse API a reçu la requête "POST /api/measures"
    And le corps JSON a le champ "type" valant "temperature"
    And le corps JSON a le champ "date" valant "2026-01-01T00:00:00"
    And le corps JSON a le champ "value" valant "21.5"
    And le corps JSON a le champ "moduleKey" valant "k-1"

  Scenario: une analyse n'a pas de clé de module
    When le driver HTTP sauvegarde l'analyse "smoke" "2026-01-01T00:00:00" "1"
    Then le driver répond "OK"
    And le corps JSON n'a pas le champ "moduleKey"

  Scenario: GETCONFIG encode la clé et renvoie l'intervalle
    Given la fausse API répond avec le JSON :
      """
      {"error":0,"status":200,"data":{"sendInterval":30}}
      """
    When le driver HTTP demande la config de la clé "a b&c"
    Then le driver répond "OK 30"
    And la fausse API a reçu la requête "GET /api/modules/config?key=a+b%26c"

  Scenario: une API injoignable donne une erreur explicite
    When la fausse API est arrêtée
    And le driver HTTP sauvegarde l'analyse "smoke" "2026-01-01T00:00:00" "1"
    Then le driver répond "ERR cannot join the API"
