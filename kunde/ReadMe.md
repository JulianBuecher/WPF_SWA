# Hinweise zum Programmierbeispiel

<Juergen.Zimmermann@HS-Karlsruhe.de>

> Diese Datei ist in Markdown geschrieben und kann z.B. mit IntelliJ IDEA
> oder NetBeans gelesen werden. Näheres zu Markdown gibt es in einem
> [Wiki](http://bit.ly/Markdown-Cheatsheet)

## Powershell

Überprüfung, ob sich Powershell-Skripte (s.u.) starten lassen:

```CMD
    Get-ExecutionPolicy -list
```

`CurrentUser` muss das Recht `RemoteSigned` haben. Ggf. muss das Ausführungsrecht ergänzt werden:

```CMD
    Set-ExecutionPolicy RemoteSigned CurrentUser
```

## Falls die Speichereinstellung für Gradle zu großzügig ist

In `gradle.properties` bei `org.gradle.jvmargs` den voreingestellten Wert
(1 GB) ggf. reduzieren.

## MongoDB in Kubernetes (de-) installieren

Falls man in MongoDB Transaktionen nutzen möchte, benötigt man MongoDB 4.x mit
der Installation eines ReplicaSet für MongoDB, was nicht verwechselt werden darf
mit einem ReplicaSet innerhalb von Kubernetes. Ein solches ReplicaSet kann durch
den _MongoDB Community Kubernetes Operator_ von
`https://github.com/mongodb/mongodb-kubernetes-operator` zwar installiert
werden, hat aber erst die Version 0.1.1.
Der _MongoDB Enterprise Operator for Kubernetes_ ist zwar ausgereift, aber
verständlicherweise nicht für Notebooks und nicht als Entwicklungsplattform
gedacht.

Deshalb wird das Docker-Image für _MongoDB_ wird in Kubernetes als ein eigenes
_StatefulSet_ installiert, indem im Unterverzeichnis `bin` das PowerShell-Skript
`.\mongodb.ps1 install` aufgerufen wird. Das _StatefulSet_ kann man durch z.B.
_Octant_ (s.u.) inspizieren. Im _Docker Dashboard_ kann man den gestarteten
Container ebenfalls sehen.

Der (interne) Kubernetes-Port für den eigenen Microservice ist der Default-Port
von MongoDB, nämlich `27017`. Wenn man diesen Port z.B. für MongoDB Compass,
d.h. extern freigeben möchte, kann man durch den Skript-Aufruf `.\mongodb.ps1`
ein Forwarding einrichten.

Durch den Skript-Aufruf `.\mongodb.ps1 uninstall` kann man MongoDB wieder
deinstallieren.

## fake-smtp-server in Kubernetes (de-) installieren

Das Docker-Image für _fake-smtp-server_ wird in Kubernetes als _Deployment_
installiert, indem im Unterverzeichnis `bin` das PowerShell-Skript
`.\mailserver.ps1 install` aufgerufen wird. Das _Deployment_ kann man durch z.B.
_Octant_ (s.u.) inspizieren. Im _Docker Dashboard_ kann man den gestarteten
Container ebenfalls sehen.

Die (internen) Kubernetes-Ports für den eigenen Microservice sind der
Default-Port 5025 für SMTP und 5080 für HTTP. Wenn man dieses Ports z.B. für
einen extern laufenden Microservice oder für einen Webbrowser freigeben möchte,
kann man durch den Skript-Aufruf `.\mailserver.ps1` ein Forwarding einrichten.

Empfangene Emails kann man dann in einem Webbrowser mit der URL
`http://localhost:5080` inspizieren, falls ein Forwarding eingerichtet wurde;
andernfalls mit der URL `http://localhost:31080`.

Durch den Skript-Aufruf `.\mailserver.ps1 uninstall` kann man den Mailserver
wieder deinstallieren.

## Übersetzung und lokale Ausführung

### Public und Private Key für HTTPS

In einer Powershell werden der Keystore `keystore.p12` und der Truststore
`truststore.p12` im Format PKCS#12 durch das nachfolgende Kommando im
Unterverzeichnis `src\main\resources erzeugt.

```CMD
    .\create-keystore.ps1
```

### Start und Stop des Servers in der Kommandozeile

In einer Powershell wird der Server mit dem Profil `dev` und der Möglichkeit
für einen _Restart_ gestartet, falls es geänderte Dateien gibt:

```CMD
    .\kunde.ps1 dev
```

In einer 2. Powershell kann man mit dem Kommando `.\kunde.ps1 stop` den Server
herunterfahren.

#### curl als REST-Client

Beispiel:

```CMD
   C:\Zimmermann\Git\mingw64\bin\curl --include --basic --user admin:p --tlsv1.3 --insecure https://localhost:8444/00000000-0000-0000-0000-000000000001
```

#### Evtl. Probleme mit dem Kotlin Daemon

Falls der _Kotlin Daemon_ beim Übersetzen nicht mehr reagiert, sollte man
alte Dateien im Verzeichnis `%USERPROFILE%\AppData\Local\kotlin\daemon` löschen.

#### Evtl. Kontinuierliches Monitoring von Dateiänderungen

In einer zweiten Powershell überwachen, ob es Änderungen gibt, so dass
die Dateien für den Server neu bereitgestellt werden müssen; dazu gehören die
übersetzten .class-Dateien und auch Konfigurationsdateien. Damit nicht bei jeder
Änderung der Server neu gestartet wird und man ständig warten muss, gibt es eine
"Trigger-Datei". Wenn die Datei `restart.txt` im Verzeichnis
`src\main\resources` geändert wird, dann wird ein _Neustart des Servers_
ausgelöst und nur dann.

Die Powershell, um kontinuierlich geänderte Dateien für den Server
bereitzustellen, kann auch innerhalb der IDE geöffnet werden (z.B. als
_Terminal_ bei IntelliJ).

```CMD
    .\gradlew classes -t
```

### Start des Servers innerhalb von IntelliJ IDEA

Im Auswahlmenü rechts oben, d.h. dort wo _Application_ steht, die erste
Option _Edit Configurations ..._ auswählen. Im Abschnitt _Environment_
sind die nachfolgenden Einträge vorzunehmen. Wenn diese Einträge
vorhanden sind kann man durch Anklicken des grünen Dreiecks rechts oben
die _Application_ bzw. den Microservice starten.

#### _VM options_

Bei _VM options_ ist der Wert
`-Djavax.net.ssl.trustStore=C:/Users/MEINE_KENNUNG/IdeaProjects/kunde/src/main/resources/truststore.p12 -Djavax.net.ssl.trustStorePassword=zimmermann`
einzutragen, wobei `MEINE_KENNUNG` durch die eigene Benutzerkennung zu
ersetzen ist.

#### _Program arguments_

Bei _Programm arguments_ ist der Wert
`--spring.config.location=classpath:/bootstrap.yml,classpath:/application.yml,classpath:/application-dev.yml`
einzutragen.

#### _Environment variables_

Bei _Environmen variables_ ist der Wert `spring.data.mongodb.password=p` einzutragen.

#### _Active Profiles_

Weiter unten beim Abschnitt _Spring Boot_ ist bei _Active Profiles_ der Wert
`dev` einzutragen und mit dem Button _OK_ abspeichern.

## Kubernetes durch Docker Desktop

### Rechnername in der Datei `hosts`

Wenn man mit Kubernetes arbeitet, bedeutet das auch, dass man i.a. über TCP
kommuniziert. Deshalb sollte man überprüfen, ob in der Datei
`C:\Windows\System32\drivers\etc\hosts` der eigene Rechnername mit seiner
IP-Adresse eingetragen ist. Zum Editieren dieser Datei sind Administrator-Rechte
notwendig.

### 'kubectl'

Das wichtigste Kommando, um mit Kubernetes zu kommunizieren, ist `kubectl`, wozu
es etliche Unterkommandos gibt, wie z.B. `kubectl apply`, `kubectl creete`,
`kubectl get` oder `kubectl describe`.

Durch das PowerShell-Skript `kunde.ps1` sind die Aufrufe von `kubectl` gekapselt
und dadurch vereinfacht.

### Namespace

In Kubernetes gibt es Namespaces ("Namensräume") wie in

- Betriebssystemen durch Verzeichnisse, z.B. in Windows oder Unix
- Programmiersprachen, z.B. durch `package` in Kotlin und Java
- Datenbanksystemen, z.B. in Oracle und PostgreSQL.

Genauso wie in Datenbanksystemen gibt es in Kubernetes _keine_ untergeordneten
Namespaces. Vor allem ist es in Kubernetes empfehlenswert für die eigene
Software einen _eigenen_ Namespace anzulegen und **NICHT** den Default-Namespace
zu benutzen.

Ein neuer Namespace, z.B. `acme`, wird durch das Kommando
`kubectl create namespace acme` angelegt. Damit man bei weiteren Aufrufen von
`kubectl` _nicht jedes Mal_ das Argument `--namespace acme` angeben muss, kann
man für den aktuellen Kubernetes-Kontext den zu verwendenden Namespace auf
`acme` setzen, indem man das Kommando
`kubectl config set-context --current --namespace acme` aufruft.

**ACHTUNG**: Nach **JEDEM** Neustart von _Docker Desktop_ und damit von
Kubernetes muss für den aktuellen Kontext der Namespace gesetzt werden.

Zur Vereinfachung gibt es deshalb im Unterverzeichnis `bin` das Skript
`namespace.ps1`. Durch den Aufruf `.\namespace.ps1 create` wird der Namespace
`acme` neu angelegt und durch `.\namespace.ps1` wird der Namespace `acme` für
den aktuellen Kontext gesetzt.

### Installation

`.\kunde.ps1 install` baut zunächst ein Image für Docker. Danach wird eine
_Configmap_ für Kubernetes erstellt, in der Umgebungsvariable zur Konfiguration
des Deployments bereitgestellt werden. Abschließend wird in Kubernetes swowohl
ein _Deployment_ mit _Configmap_ und _Secret_ (für das Passwort von MongoDB)
als auch ein _Service_ für ggf. externen Zugriff erstellt. Die dazu notwendige
Manifest- bzw. Konfigurationsdatei ist `kunde.yml`.

Externer Zugriff ist beim Entwickeln und Testen relativ einfach durch
_Port-Forwarding_ möglich, indem das Skript `.\kunde.ps1 forward` aufgerufen
wird. Man kann auch dauerhaft einen extern zugänglichen Port definieren, indem
in der Manifestdatei _NodePort_ auf z.B. `30080` gesetzt wird.

### Octant vs. Kubernetes Dashboard vs. Kubernetes-Extension für VS Code

#### Kubernetes Dashboard

_Kubernetes Dashboard_ ist vor allem für typische Administrationsaufgaben
geeignet und kann durch
`kubectl apply --filename https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.3/aio/deploy/recommended.yaml`
installiert werden. Bevor man das Dashboard über die URL
`http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy`
aufrufen kann, muss man in einer PowerShell das Kommando `kubectl proxy`
eingeben. In der Startseite muss man einen Token eingeben, den man über eine
PowerShell folgendermaßen ermittelt:

```CMD
    $tokenName = kubectl get secrets --namespace kubernetes-dashboard | C:\Zimmermann\Git\usr\bin\awk '/^kubernetes-dashboard-token-/ {print $1}'
    kubectl describe secret $tokenName --namespace kubernetes-dashboard | C:\Zimmermann\Git\usr\bin\awk '/^token:/ {print $2}'
```

Mit dem Kommando
`kubectl delete --filename https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.3/aio/deploy/recommended.yaml`
lässt sich das Kubernetes Dashboard deinstallieren.

#### Kubernets-Extension für VS-Code

Für _VS Code_ gibt es die Extension _Kubernetes_ von Microsoft. Diese
Extension ist ähnlich wie _Octant_ auf die Bedürfnisse der Entwickler/innen
zugeschnitten und ermöglicht den einfachen Zugriff auf ein Terminal oder
die Logs.

#### Octant

_Octant_ ist vor allem für Entwickler/innen geeignet. Octant muss
von einer PowerShell aus gestartet werden, in dem man im Verzeichnis
`C:\Zimmermann\octant` das Kommando `.\octant.exe` aufruft.

Zunächst stellt man rechts oben, den Namespace von `default` auf `acme`. Danach
hat man unter _Namespace Overview_ eine Übersicht über z.B. _Pods_,
_Deployments_, _Stateful Sets_, _Services_, _Config Maps_ und _Secrets_.

In der Navigationsleiste am linken Rand findet gezielt man bei

- Workloads: Pods, Deployments, Stateful Sets
- Discovery and Loadbalancing: Services
- Config and Storage: Config Maps und Secrets

Bei einem Pod als Laufzeitumgebung hat man direkten Zugriff auf

- die Logging-Ausgaben in der Konsole der virtualisierten Software
- ein Terminal mit einer Shell durch `bash`, falls das zugrundeliegende
  Docker-Image die bash enthält, wie z.B. bei MongoDB, wo man dann im Laufe des
  Semesters DB-Queries mit dem Kommandozeilen-Client absetzen kann.
  Das Docker-Image für die eigenen Microservices enthält keine bash.

### Deinstallieren

Mit `.\kunde.ps1 uninstall` werden die installierten _Service_, _Deployment_,
_Configmap_ und _Secret_ wieder aus Kubernetes entfernt, weshalb implizit auch
das _Pod_ entfernt wird.

### Aktualisieren des Deployments

Wenn man den eigenen Code für den Microservice modifiziert und dann ein neues
Docker-Image baut, dann kann man das neu gebaute Docker-Image bei den Images im
_Docker Dashboard_ sehen.

Was dabei aber i.a. nicht geändert wurde ist die Manifest-Datei (hier:
`kunde.yml`). Ein Deployment wäre aus technischer Sicht zwar erfolgreich, weil
es keine Fehlermeldung gibt, aber die Statusmeldung `unchanged` wäre nicht aus
Entwicklersicht sicher nicht das gewünschte Resultat. Deshalb muss zunächst das
Deployment entfernt werden, damit das alte, nicht mehr benötigte Docker-Image
gelöscht werden kann. Nun kann ein neues Docker-Image gebaut und in einem
Deployment an Kubernetes übergeben werden. Alle diese Schritte werden
ausgeführt, wenn man das Skript `.\kunde.ps1 redeploy` aufruft.

Falls das nicht mehr benötigte Docker-Image wegen "Race Conditions" nicht
gelöscht wurde, kann man sich mit `docker image ls` sämtliche Images auflisten
lassen und mit `docker image rm ...` die Images mit einem Tag `<none>` löschen,
indem man ihre IDs verwendet.

## Tests

Folgende Server müssen gestartet sein:

- MongoDB
- Mailserver

```CMD
    .\gradlew test --fail-fast [--rerun-tasks]
```

## Codeanalyse durch ktlint und detekt

```CMD
    .\gradlew ktlint detekt
```

## Entwicklerhandbuch im HTML- und PDF-Format

```CMD
    .\gradlew asciidoctor asciidoctorPdf
```

## API-Dokumentation

```CMD
    .\gradlew dokka
```
