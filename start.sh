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

# 自动加载 .env
if [ -f .env ]; then
  set -a
  source .env
  set +a
  echo "    环境变量已从 .env 导入"
else
  echo "\033[31mERROR: 未找到 .env 文件，无法自动导入 REDIS_HOST 和 REDIS_PASSWORD\033[0m"
  exit 1
fi

# 检查关键环境变量是否设置
if [[ -z "${REDIS_HOST}" ]]; then
  echo "\033[31mERROR: REDIS_HOST 未设置或为空，请在 .env 文件中补充\033[0m"
  exit 1
fi
if [[ -z "${REDIS_PASSWORD}" ]]; then
  echo "\033[31mERROR: REDIS_PASSWORD 未设置或为空，请在 .env 文件中补充\033[0m"
  exit 1
fi

# 启动容器
docker run --name "${CONTAINER}" -i --rm -d -p 8003:8080 \
  -e REDIS_HOST \
  -e REDIS_PASSWORD \
  "${IMAGE}"

echo "[  SUCCESS  ] 新容器已启动。（REDIS_HOST=${REDIS_HOST}）"