#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION_FILE="${ROOT_DIR}/VERSION"

NAMESPACE="rlarbdud"
PLATFORMS="linux/amd64,linux/arm64"
PLATFORM="linux/amd64"


CURRENT_VERSION="$(cat "${VERSION_FILE}")"

IFS='.' read -r MAJOR MINOR PATCH <<< "${CURRENT_VERSION}"
NEXT_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))"

echo "Release version: ${NEXT_VERSION}"

build_and_push() {
  IMAGE_NAME="$1"
  CONTEXT="$2"
  DOCKERFILE="$3"

  IMAGE="${NAMESPACE}/${IMAGE_NAME}"

  docker buildx build \
    --platform "${PLATFORMS}" \
    -f "${DOCKERFILE}" \
    -t "${IMAGE}:${NEXT_VERSION}" \
    -t "${IMAGE}:latest" \
    --push \
    "${CONTEXT}"
}
build_and_pushA() {
  IMAGE_NAME="$1"
  CONTEXT="$2"
  DOCKERFILE="$3"

  IMAGE="${NAMESPACE}/${IMAGE_NAME}"

  docker buildx build \
    --platform "${PLATFORM}" \
    -f "${DOCKERFILE}" \
    -t "${IMAGE}:${NEXT_VERSION}" \
    -t "${IMAGE}:latest" \
    --push \
    "${CONTEXT}"
}

