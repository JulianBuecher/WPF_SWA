# Copyright (C) 2019 - present Juergen Zimmermann, Hochschule Karlsruhe
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

Param (
    [string] $cmd = ''
)

Set-StrictMode -Version Latest

$versionMinimum = [Version]'7.1.0'
$versionCurrent = $PSVersionTable.PSVersion
if ($versionMinimum -gt $versionCurrent) {
    throw "PowerShell $versionMinimum statt $versionCurrent erforderlich"
}

$mongoDbPort = '27017'
$smtpPort = '5025'
$httpPort = '5080'
$namespace = 'acme'

function Install-Mailserver-MongoDB {
    # Secret: password als Key, Passwort als Value mit Base64-Codierung
    # fuer Umgebungsvariable MONGO_INITDB_ROOT_PASSWORD und SPRING_DATA_MONGODB_PASSWORD
#    $password = 'p'
#    kubectl create secret generic mongodb-password-dev --from-literal password=$password --namespace $namespace

    kubectl apply --filename backend.yml
    Write-Output ''
    Write-Output 'Passwort fuer MongoDB ueberpruefen:'
    Write-Output "    kubectl describe secret mongodb-password-dev --namespace $namespace"
    Write-Output ''
    Write-Output "    `$base64 = kubectl get secret mongodb-password-dev --output jsonpath=`'{.data.password}`' --namespace $namespace"
    Write-Output '    $pwd = [Convert]::FromBase64String($base64)'
    Write-Output '    [System.Text.Encoding]::UTF8.GetString($pwd)'
    Write-Output ''
    Write-Output "    `$podName = kubectl get pods --selector app=mongodb --namespace $namespace | C:\Zimmermann\Git\usr\bin\awk `'/^mongodb-/ {print `$1}`'"
    Write-Output "    kubectl exec `$podName --stdin --tty --namespace $namespace -- printenv MONGO_INITDB_ROOT_PASSWORD"
    Write-Output "    kubectl exec `$podName --container mongo --stdin --tty --namespace $namespace -- printenv MONGO_INITDB_ROOT_PASSWORD"
    Write-Output ''
    Write-Output 'Port-Forwarding waehrend der Entwicklung:'
    Write-Output "    kubectl port-forward service/mailserver $smtpPort $httpPort --namespace $namespace"
    Write-Output "        Dashboard Mailserver:    http://localhost:$httpPort"
    Write-Output "    kubectl port-forward service/mongodb $mongoDbPort --namespace $namespace"
    Write-Output ''
}

function Uninstall-Mailserver-MongoDB {
    kubectl config set-context --current --namespace $namespace

    kubectl delete deployment,service mailserver
    kubectl delete configmap mailserver-env-dev

    kubectl delete service,statefulset mongodb
    kubectl delete persistentvolumeclaim mongodb-volume-mongodb-0
    kubectl delete persistentvolume mongodb-volume
    kubectl delete configmap mongodb-env-dev
    kubectl delete secret mongodb-password-dev
}

function Set-Forward-Mailserver {
    kubectl port-forward service/mailserver $smtpPort $httpPort --namespace $namespace
}

function Set-Forward-MongoDB {
    kubectl port-forward service/mongodb $mongoDbPort --namespace $namespace
}

function Convert-Base64() {
    $str = 'p'
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($str)
    [System.Convert]::ToBase64String($bytes)
}

switch ($cmd) {
    'install' { Install-Mailserver-MongoDB; break }

    'uninstall' { Uninstall-Mailserver-MongoDB; break }

    'forward-mailserver' {
        $host.ui.RawUI.WindowTitle = 'Mailserver'
        Set-Forward-Mailserver;
        break
    }
    'forward-mongodb' {
        $host.ui.RawUI.WindowTitle = 'MongoDB'
        Set-Forward-MongoDB;
        break
    }

    'base64' { Convert-Base64; break }

    default {
        $script = $myInvocation.MyCommand.Name
        Write-Output "$script [install|uninstall|forward-mailserver|forward-mongodb|base64]"
    }
}
