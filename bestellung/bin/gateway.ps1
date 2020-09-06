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

# Aufruf:   .\kunde.ps1 [install|uninstall]

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

function Install-Gateway-VirtualService {
    kubectl apply --filename gateway.yml
    Write-Output ''
    Write-Output 'Beispiel-Aufruf mit curl:'
    Write-Output "    C:\Zimmermann\Git\mingw64\bin\curl --silent --user admin:p http://localhost/bestellungen/api/10000000-0000-0000-0000-000000000001"
    Write-Output ''
}

function Uninstall-Gateway-VirtualService {
    $namespace = 'acme'
    kubectl delete virtualservice.networking.istio.io/acme-bestellung --namespace $namespace
}

# https://docs.microsoft.com/en-us/powershell/scripting/developer/cmdlet/approved-verbs-for-windows-powershell-commands?view=powershell-7#invoke-vs-start
switch ($cmd) {
    'install' { Install-Gateway-VirtualService }
    'uninstall' { Uninstall-Gateway-VirtualService }

    default {
        $script = $myInvocation.MyCommand.Name
        Write-Output "$script [cmd=install|uninstall]"
    }
}
