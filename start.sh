#!/bin/zsh
set -e

echo "[1] git fetch --all && git reset --hard origin/master"
git fetch --all && git reset --hard origin/master

echo "[2] Gradle 原生构建"
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false

CONTAINER=blogapi
IMAGE=quarkus/ahogek-blog-quarkus

echo "[3] 检查并删除同名容器(如有)"
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER}\$"; then
    echo "    正在停止并删除旧容器: ${CONTAINER}"
    docker stop "${CONTAINER}"
else
    echo "    没有同名旧容器，无需删除"
fi

echo "[4] 检查并删除同名镜像(如有)"
if docker images --format '{{.Repository}}' | grep -q "^${IMAGE}\$"; then
    echo "    删除镜像: ${IMAGE}"
    docker rmi "${IMAGE}"
else
    echo "    没有同名镜像，无需删除"
fi

echo "[5] 构建新镜像"
docker build -f src/main/docker/Dockerfile.native -t "${IMAGE}" .

echo "[6] 启动新容器"
docker run --name "${CONTAINER}" -i --rm -p 8003:8080 "${IMAGE}"