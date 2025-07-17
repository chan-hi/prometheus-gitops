#!/bin/bash

# Update image tags in Kubernetes manifests and trigger ArgoCD sync
# Usage: ./update-image-tag.sh <service-name> <image-tag>

SERVICE_NAME=$1
IMAGE_TAG=$2
ECR_REPO="557690584596.dkr.ecr.ap-northeast-2.amazonaws.com/gitops/msa-app"
MANIFEST_FILE="msa-demo/k8s-manifests-final.yaml"

if [ -z "$SERVICE_NAME" ] || [ -z "$IMAGE_TAG" ]; then
    echo "Usage: $0 <service-name> <image-tag>"
    echo "Example: $0 user-service v1.0.0"
    exit 1
fi

# Update the image tag in the manifest
sed -i "s|${ECR_REPO}:${SERVICE_NAME}-.*|${ECR_REPO}:${SERVICE_NAME}-${IMAGE_TAG}|g" ${MANIFEST_FILE}

echo "Updated ${SERVICE_NAME} image tag to ${IMAGE_TAG} in ${MANIFEST_FILE}"

# Commit and push changes
git add ${MANIFEST_FILE}
git commit -m "Update ${SERVICE_NAME} image tag to ${IMAGE_TAG}"
git push

echo "Changes committed and pushed to repository"