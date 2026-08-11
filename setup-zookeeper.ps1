$ZookeeperVersion = "3.9.2"
$ZookeeperDir = Join-Path $PSScriptRoot "zookeeper-server"
$TarFile = Join-Path $ZookeeperDir "apache-zookeeper-$ZookeeperVersion-bin.tar.gz"
$DownloadUrl = "https://archive.apache.org/dist/zookeeper/zookeeper-$ZookeeperVersion/apache-zookeeper-$ZookeeperVersion-bin.tar.gz"

if (-not (Test-Path $ZookeeperDir)) {
    New-Item -ItemType Directory -Path $ZookeeperDir | Out-Null
}

Write-Host "Checking for local ZooKeeper $ZookeeperVersion..."
$ExtractedDir = Join-Path $ZookeeperDir "apache-zookeeper-$ZookeeperVersion-bin"
if (Test-Path $ExtractedDir) {
    Write-Host "ZooKeeper is already downloaded and extracted in $ExtractedDir"
} else {
    Write-Host "Downloading ZooKeeper from $DownloadUrl..."
    try {
        Invoke-WebRequest -Uri $DownloadUrl -OutFile $TarFile -UseBasicParsing
        Write-Host "Download complete. Extracting..."
        
        # Extract the tar.gz file using native tar
        # tar requires us to be in the target directory or use -C
        tar -xf $TarFile -C $ZookeeperDir
        
        # Clean up the downloaded tar file to save space
        Remove-Item $TarFile -Force
        Write-Host "Extraction complete. Configuring..."
    } catch {
        Write-Error "Failed to download or extract ZooKeeper: $_"
        exit 1
    }
}

# Create a basic configuration file if it doesn't exist
$ConfDir = Join-Path $ExtractedDir "conf"
$CfgFile = Join-Path $ConfDir "zoo.cfg"
$DataDir = (Join-Path $ZookeeperDir "data").Replace("\", "/")

if (-not (Test-Path $CfgFile)) {
    Write-Host "Creating zoo.cfg configuration..."
    $ConfigContent = @"
tickTime=2000
dataDir=$DataDir
clientPort=2181
initLimit=5
syncLimit=2
admin.enableServer=false
"@
    Set-Content -Path $CfgFile -Value $ConfigContent
    Write-Host "Configuration created at $CfgFile"
} else {
    Write-Host "zoo.cfg already exists."
}

Write-Host "ZooKeeper is successfully set up and ready to run!"
