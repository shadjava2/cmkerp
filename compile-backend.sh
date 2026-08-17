#!/bin/bash
# Script de compilation du backend pour résoudre l'erreur PageResponse
# Usage: ./compile-backend.sh

echo "========================================"
echo "Compilation du backend CMK-ERP"
echo "========================================"
echo ""

# Aller à la racine du projet
cd "$(dirname "$0")"

echo "[1/3] Nettoyage des modules..."
mvn clean -DskipTests
if [ $? -ne 0 ]; then
    echo "ERREUR: Le nettoyage a échoué"
    exit 1
fi

echo ""
echo "[2/3] Compilation et installation de cmkerp-shared-kernel..."
cd cmkerp-shared-kernel
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "ERREUR: La compilation de cmkerp-shared-kernel a échoué"
    cd ..
    exit 1
fi
cd ..

echo ""
echo "[3/3] Compilation et installation de cmkerp-stocks..."
cd cmkerp-stocks
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "ERREUR: La compilation de cmkerp-stocks a échoué"
    cd ..
    exit 1
fi
cd ..

echo ""
echo "========================================"
echo "Compilation terminée avec succès!"
echo "========================================"
echo ""
echo "Vous pouvez maintenant redémarrer le backend Spring Boot."
echo ""

