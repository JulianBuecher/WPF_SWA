# Copyright (C) 2017 - present Juergen Zimmermann, Hochschule Karlsruhe
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

Set-StrictMode -Version Latest

$versionMinimum = [Version]'7.1.0'
$versionCurrent = $PSVersionTable.PSVersion
if ($versionMinimum -gt $versionCurrent) {
    throw "PowerShell $versionMinimum statt $versionCurrent erforderlich"
}

# Titel setzen
$host.ui.RawUI.WindowTitle = $myInvocation.MyCommand.Name

$version = '11.0.1'
$port = '9900'
# docker pull jboss/keycloak:$version
# http://localhost:5080

Write-Output ''
Write-Output "keycloak $version wird als Docker-Container gestartet"
Write-Output "URL fuer das Dashboard:   http://localhost:$port/auth/admin"
Write-Output ''

# https://www.keycloak.org/getting-started/getting-started-docker
$keycloakDir = 'C:\Zimmermann\volumes\keycloak'
#docker run --publish ${port}:8080 `
#    --env TZ=Europe/Berlin `
#    --env KEYCLOAK_USER=keycloak `
#    --env KEYCLOAK_PASSWORD=p `
#    --name keycloak --rm -d `
#    jboss/keycloak:$version

# docker exec keycloak ls -l /opt/jboss/keycloak/standalone/data

# For exporting the Keycloak realm
#docker exec -it keycloak /opt/jboss/keycloak/bin/standalone.sh `
#-Dkeycloak.migration.action=export `
#-Dkeycloak.migration.realmName=swa `
#-Dkeycloak.migration.userExportStrategy=REALM_FILE `
#-Dkeycloak.migration.file=/tmp/swa_realm.json

# Copying the exported file out of the container
#$containerId = "Container ID"
#docker cp ${containerId}:/tmp/swa_realm.json c:/WPF_SWA/workspace/beispiel-2/kunde/bin

# Run Keycloak Container with existing realm file
docker run -p 9900:8080 `
    --mount type=bind,source=${keycloakDir}\keycloak.mv.db,destination=/opt/jboss/keycloak/standalone/data/keycloak.mv.db `
    --mount type=bind,source=${keycloakDir}\keycloak.trace.db,destination=/opt/jboss/keycloak/standalone/data/keycloak.trace.db `
    --name keycloak `
    -e TZ=Europe/Berlin `
    -e KEYCLOAK_USER=keycloak `
    -e KEYCLOAK_PASSWORD=p `
    -e JAVA_OPTS=" -Dkeycloak.profile.feature.scripts=enabled -Dkeycloak.profile.feature.upload_scripts=enabled" `
    --rm -d `
    jboss/keycloak:11.0.2
