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

# Aufruf:   .\kunde.ps1 [start|dev|stop|install|uninstall|redeploy|forward]

# "Param" muss in der 1. Zeile sein
Param (
    [string]$cmd = 'start'
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

$microservice = 'kunde'
$imageVersion = '1.0'
$port = '8080'
$namespace = 'acme'

function Invoke-Server($dev = '') {
    if ($dev -eq '') {
        ./gradlew bootRun
    } else {
        ./gradlew bootRun -Ddev
    }
}

function Complete-Server {
    $schema = 'https'
    $tls = '--tlsv1.3'

    #$http = '--http3'
    $http = '--http2'

    $username = 'admin'
    $password = 'p'

    $url = "${schema}://localhost:${port}/actuator/shutdown"
    $basicAuth = "${username}:${password}"

    # cURL = Client for URLs / Curl URL Request Library
    C:\Zimmermann\Git\mingw64\bin\curl --version
    # ggf. --include statt --verbose
    C:\Zimmermann\Git\mingw64\bin\curl --verbose --request POST --basic --user $basicAuth $http $tls --insecure $url
}

function Install-Service-Deployment-Configmap {
    .\gradlew bootBuildImage
    #.\gradlew jibDockerBuild
    Write-Output ''

    kubectl apply --filename $microservice`.yml
    Write-Output ''
    Write-Output 'Beispiel-Aufruf mit curl:'
    Write-Output "    C:\Zimmermann\Git\mingw64\bin\curl --silent --user admin:p http://localhost:${port}/api/00000000-0000-0000-0000-000000000001"
    Write-Output "    C:\Zimmermann\Git\mingw64\bin\curl --verbose --header `'If-None-Match:`\`"0`\`"`' --user admin:p http://localhost:${port}/api/00000000-0000-0000-0000-000000000001"
    Write-Output "    C:\Zimmermann\Git\mingw64\bin\curl --silent --user admin:p http://localhost:${port}/home"
    Write-Output ''

    $configmap = "$microservice-env-dev"
    Write-Output "Umgebungsvariable in der Configmap ${configmap}:"
    Write-Output "    kubectl describe configmap $configmap --namespace $namespace"
    Write-Output "    kubectl get configmap $configmap --output jsonpath='{.data}' --namespace $namespace"
    Write-Output ''
}

function Uninstall-Service-Deployment-Configmap {
    kubectl config set-context --current --namespace $namespace

    kubectl delete deployment,service $microservice
    docker image rm ${microservice}:$imageVersion
    kubectl delete configmap $microservice-env-dev
}

function Update-Deployment {
    kubectl config set-context --current --namespace $namespace

    kubectl delete deployment $microservice
    docker image rm ${microservice}:$imageVersion
    .\gradlew bootBuildImage
    #.\gradlew jibDockerBuild
    kubectl apply --filename $microservice`.yml
}

function Set-Forward {
    kubectl port-forward service/$microservice $port --namespace $namespace
}

# https://docs.microsoft.com/en-us/powershell/scripting/developer/cmdlet/approved-verbs-for-windows-powershell-commands?view=powershell-7#invoke-vs-start
switch ($cmd) {
    'start' { Invoke-Server; break }
    'dev' { Invoke-Server -dev 'dev'; break }
    'stop' { Complete-Server; break }

    'install' { Install-Service-Deployment-Configmap }
    'uninstall' { Uninstall-Service-Deployment-Configmap }
    'redeploy' { Update-Deployment }
    'forward' { Set-Forward; break }

    default { Write-Output "$script [cmd=start|dev|stop|install|uninstall|redeploy|forward]" }
}
