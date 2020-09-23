# Copyright (C) 2017 -  Juergen Zimmermann, Hochschule Karlsruhe
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# https://docs.microsoft.com/en-us/powershell/scripting/developer/cmdlet/approved-verbs-for-windows-powershell-commands?view=powershell-7

# Aufruf:   .\microservice.ps1 forward [kunde|mongodb|mailserver]|start|stop

# "Param" muss in der 1. Zeile sein
Param (
    [string]$cmd = 'help',
    [string]$service = 'kunde'
)

Set-StrictMode -Version Latest

$versionMinimum = [Version]'7.1.0'
$versionCurrent = $PSVersionTable.PSVersion
if ($versionMinimum -gt $versionCurrent) {
    throw "PowerShell $versionMinimum statt $versionCurrent erforderlich"
}

# Titel setzen
$script = $myInvocation.MyCommand.Name
$host.ui.RawUI.WindowTitle = $script

$port = '8080'
$namespace = 'acme'

function Install-Helmfile {
    # Chart-Namen: mailserver, mongodb, microservice
    # Chart-Verzeichnis mit der Chart-Definition: im gleichnamigen Unterverzeichnis
    # Release-Name = mailserver oder mongodb oder kunde oder bestellung

    # helm install mailserver .\helm-charts\mailserver
    # helm uninstall mailserver
    # helm install mongodb .\helm-charts\mongodb
    # helm install gateway .\helm-charts\gateway
    # helm install kunde --set gateway.plural.suffix=n .\helm-charts\microservice
    # helm install bestellung .\helm-charts\microservice
    #   ggf. --dry-run --debug

    Write-Output 'gradle bootBuildImage'
    Write-Output 'helmfile apply'
}

function Set-Forward {
    switch ($service) {
        'kunde' { kubectl port-forward service/$service $port --namespace $namespace; break }
        'mongodb' { kubectl port-forward service/$service 27017 --namespace $namespace; break }
        'mailserver' { kubectl port-forward service/$service 5025 5080 --namespace $namespace; break }
    }

}

function Invoke-Server {
    ./gradlew bootRun -Ddev
}

function Complete-Server {
    $schema = 'https'
    $tls = '--tlsv1.3'

    #$http = '--http3'
    $http = '--http2'

    $username = 'admin'
    $password = 'p'

    $url = "${schema}://localhost:$port/actuator/shutdown"
    $basicAuth = "${username}:${password}"

    # cURL = Client for URLs / Curl URL Request Library. Ggf. --include statt --verbose
    curl --verbose --request POST --basic --user $basicAuth $http $tls --insecure $url
}

# https://docs.microsoft.com/en-us/powershell/scripting/developer/cmdlet/approved-verbs-for-windows-powershell-commands?view=powershell-7#invoke-vs-start
switch ($cmd) {
    'install' { Install-Helmfile; break }
    'forward' { Set-Forward; break }

    'start' { Invoke-Server; break }
    'stop' { Complete-Server; break }

    default { Write-Output "$script install|forward [kunde|mongodb|mailserver]|start|stop" }
}
