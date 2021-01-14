# Repository für das WPF in Softwarearchitektur

## Table of Contents

- [Verwendung](#verwendung)
- [Resourcen](#resourcen)
- [Umsetzung](#umsetzung)
- [Weitere Änderungen](#weitere-änderungen)

## Verwendung

Um das Projekt zu nutzen, muss Docker laufen und Kubernetes aktiviert sein.
Außerdem müssen die Dateien im Ordner `keycloak-dateien` in den Ordner `C:\Zimmermann\volumes\keycloak` verschoben werden.

1. Starte das Backend, Keycloak und die beiden Microservices

    ```ps1
    > cd [...]\beispiel-2\kunde\bin
    > .\backend.ps1 install
    > .\backend.ps1 forward-mongodb
    > .\backend.ps1 forward-mailserver
    > .\keycloak.ps1 install
    > .\keycloak.ps1 forward-keycloak
    ```

    ```ps1
    > cd [...]\beispiel-2\kunde
    > .\kunde.ps1 install
    > .\kunde.ps1 forward
    ```

    ```ps1
    > cd [...]\beispiel-2\bestellung
    > .\bestellung.ps1 install
    > .\bestellung.ps1 forward
    ```

2. Frage ein Access Token an
   - POST-Request an `http://localhost:9900/auth/realms/swa/protocol/openid-connect/token` mit folgenden Key-Value Paaren:
     - `grant_type: password`
     - `client_id: kunde` oder `bestellung`
     - `username: student`, `admin` oder `kunde`
     - `password: p`

   - cURL Beispiel:

      ```ps1
      > curl --location --request POST 'http://localhost:9900/auth/realms/swa/protocol/openid-connect/token' 
      --header 'Content-Type: application/x-www-form-urlencoded' 
      --data-urlencode 'grant_type=password' 
      --data-urlencode 'client_id=kunde' 
      --data-urlencode 'username=student' 
      --data-urlencode 'password=p'
      ```

3. Mit dem erhaltenen Access Token Requests an den Kunde bzw. Bestellung Microservice schicken
   - bspw. `https://localhost:8080/api/00000000-0000-0000-0000-000000000001`
   - im Header Token mitgeben: `Authorization: <Token>`

## Resourcen

Zur Umsetzung des WPFs soll Keycloak und die Authentifizierung mittels OpenID-Connect in die bestehenden Services **kunde** und **bestellung** eingefügt werden.  
Anbei einige interessante Online-Ressourcen für die Lektüre:

- <https://github.com/thomasdarimont>
- <https://springone.io/2020/sessions/explain-it-to-me-like-im-5-oauth2-and-openid>
- <https://github.com/spring-projects/spring-security/wiki/OAuth-2.0-Features-Matrix>
- <https://github.com/spring-projects/spring-security/tree/5.4.0-RC1/samples/boot/oauth2login>
- <http://www.baeldung.com/spring-security-5-oauth2-login>
- <https://dzone.com/articles/oauth-20-in-a-nutshell>
- <https://developer.okta.com/blog/2017/06/21/what-the-heck-is-oauth>
- <https://dzone.com/articles/secure-spring-rest-with-spring-security-and-oauth2>
- <https://dzone.com/articles/json-web-tokens-with-spring-cloud-microservices> 

## Umsetzung

Um die Umstellung auf OAuth zu erleichtern war unser erster Schritt das Entfernen bzw. Umschreiben von Klassen und Funktionen, die mit der Authentifizierung zu tun haben.  
Dies betraf u. A. die Rollen, die AuthorizationConfig und den AuthorizationHandler, ebenso wie die Service und Handler Klassen.

Danach wurde bei einem Request der Kunde, falls er vorhanden war, zurückgegeben. Falls nicht wurde `NotFound` zurückgegeben.

Als Nächstes wurde Spring Security OAuth 2.0 hinzugefügt und in der `AuthorizationConfig` implementiert und der Zugriff auf die API Pfade für bestimmte Rollen freigegeben.
Die `AuthorizationConfig` referenziert hierbei auf einen *Resource Server* (Keycloak) zum Autorisieren der Rollen und zum Ausstellen der JWT Token.

In Keycloak haben wir ein Realm zum Verwalten der Rollen (*Kunde, Student und Admin*) und Clients (*Kunde* und *Bestellung*) angelegt.
Über Keycloak kann somit von einem Client für eine bestimmte Rolle ein Token angefragt werden, welches dann für die weitere Autorisierung genutzt wird.

Für die Verwaltung von Keycloak im Kubernetes Cluster wurde ebenfalls ein Powershell-Skript geschrieben, über das Keycloak im Cluster de- bzw. installiert werden kann oder die Portweiterleitung aktiviert werden kann.
Außerdem wurde für die Installation eine Deployment Konfiguration geschrieben, die unter Anderem auf `Zimmermann/volumes/keycloak` als *persistentVolume* für das Speichern der Konfigurationsdaten verweist.

Nachdem die Autorisierung im Kunde Microservice funktioniert hat, wurde der Bestellung Microservice auf dieselbe Weise umgeschrieben.  
Da der BestellungService ebenfalls auf Daten des Kunden zugreifen muss und dies autorisiert werden muss, wird der Token aus dem Header im Request des `BestellungHandler` geparst und an die Funktionen im `BestellungService` weitergegeben.
Der `BestellungService` hängt diesen Token dann als Header im Request an den Kunde Microservice.
Dies bietet den Vorteil, dass in der `AuthorizationConfig` des Kunde Microservice definiert werden kann, auf welche Pfade der Bestellung Mircoservice zugreifen kann und nicht erneut ein Token für einen Request angefragt werden muss.

Für `beispiel-3` wurde außerdem für Keycloak ein Helm-Chart angelegt, da dies die Verwaltung des Kubernetes Clusters deutlich vereinfacht.

## Weitere Änderungen

Da Spring den anonymen Zugriff auf `https://repo.spring.io` beschränkt hat ([Link zum Beitrag](https://spring.io/blog/2020/10/29/notice-of-permissions-changes-to-repo-spring-io-fall-and-winter-2020#january-6-2021)), wurden die Repositories dahingehend angepasst.
