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

# Aufruf:   .\bestellung.ps1 [install|uninstall|redeploy]

# "Param" muss in der 1. Zeile sein
Param (
    [string]$cmd = 'install'
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

$microservice = 'bestellung'
$imageVersion = '1.0'
$port = '8081'
$namespace = 'acme'

function Install-Service-Deployment-Configmap {
    .\gradlew bootBuildImage
    Write-Output ''

    kubectl apply --filename $microservice`.yml
    Write-Output ''
    Write-Output 'Beispiel-Aufruf mit curl:'
    Write-Output "    C:\Zimmermann\Git\mingw64\bin\curl --silent --user admin:p http://localhost:${port}/api/10000000-0000-0000-0000-000000000001"
    Write-Output "    C:\Zimmermann\Git\mingw64\bin\curl --silent --user admin:p http://localhost/bestellungen/api/10000000-0000-0000-0000-000000000001"
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
    kubectl apply --filename $microservice`.yml
}

function Set-Forward {
    kubectl port-forward service/$microservice ${port}:8080 --namespace $namespace
}

switch ($cmd) {
    'install' { Install-Service-Deployment-Configmap }
    'uninstall' { Uninstall-Service-Deployment-Configmap }
    'redeploy' { Update-Deployment }
    'forward' { Set-Forward; break }

    default { Write-Output "$script [install|uninstall|redeploy]" }
}
