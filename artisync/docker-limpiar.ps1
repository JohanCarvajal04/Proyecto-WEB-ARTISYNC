# Elimina contenedores, imagenes y volumenes de ArtiSync en Docker.
# Uso: powershell -File docker-limpiar.ps1  (o simplemente .\docker-limpiar.ps1 desde esta carpeta)

Set-Location $PSScriptRoot

$composeFiles = @(
    "docker-compose.yml",
    "docker-compose.azure.yml",
    "docker-compose.lighthouse.yml",
    "docker-compose.medicion.yml"
)

foreach ($f in $composeFiles) {
    if (Test-Path $f) {
        Write-Host "==> docker compose down ($f)"
        docker compose -f $f down --rmi all --volumes --remove-orphans
    }
}

# Por si quedan recursos sueltos con el prefijo pfc_ (contenedores/volumenes)
# o artisync (nombre del proyecto/imagenes locales).
$containers = docker ps -a --filter "name=pfc_" -q
if ($containers) { docker rm -f $containers }

$images = docker images --filter "reference=artisync*" -q
if ($images) { docker rmi -f $images }

$volumes = docker volume ls --filter "name=pfc_" -q
if ($volumes) { docker volume rm $volumes }

$volumes2 = docker volume ls --filter "name=artisync" -q
if ($volumes2) { docker volume rm $volumes2 }

Write-Host "==> Limpieza de Docker para ArtiSync completada."
