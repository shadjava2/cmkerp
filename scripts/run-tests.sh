#!/bin/bash

# ==========================================
# Script d'exécution des tests
# ==========================================
# Exécute les tests frontend (Vitest) et backend (JUnit 5)
#
# Usage:
#   ./run-tests.sh [frontend|backend|all]
# ==========================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/cmkerp-frontend"
BACKEND_DIR="$PROJECT_ROOT/cmkerp-stocks"

# Couleurs pour la sortie
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction pour exécuter les tests frontend
run_frontend_tests() {
  echo -e "${BLUE}🧪 Exécution des tests Frontend (Vitest)...${NC}"
  cd "$FRONTEND_DIR"

  if [ ! -f "package.json" ]; then
    echo -e "${RED}❌ package.json non trouvé dans $FRONTEND_DIR${NC}"
    exit 1
  fi

  echo -e "${YELLOW}📦 Installation des dépendances...${NC}"
  npm install --silent

  echo -e "${YELLOW}🧪 Exécution des tests...${NC}"
  npm run test || {
    echo -e "${RED}❌ Les tests frontend ont échoué${NC}"
    exit 1
  }

  echo -e "${GREEN}✅ Tests frontend terminés avec succès${NC}"
}

# Fonction pour exécuter les tests backend
run_backend_tests() {
  echo -e "${BLUE}🧪 Exécution des tests Backend (JUnit 5)...${NC}"
  cd "$BACKEND_DIR"

  if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ pom.xml non trouvé dans $BACKEND_DIR${NC}"
    exit 1
  fi

  echo -e "${YELLOW}🧪 Exécution des tests Maven...${NC}"
  mvn test -DskipTests=false || {
    echo -e "${RED}❌ Les tests backend ont échoué${NC}"
    exit 1
  }

  echo -e "${GREEN}✅ Tests backend terminés avec succès${NC}"
}

# Fonction pour exécuter tous les tests
run_all_tests() {
  echo -e "${BLUE}🚀 Exécution de tous les tests...${NC}\n"

  run_frontend_tests
  echo ""
  run_backend_tests

  echo -e "\n${GREEN}✅ Tous les tests terminés avec succès${NC}"
}

# Main
case "${1:-all}" in
  frontend)
    run_frontend_tests
    ;;
  backend)
    run_backend_tests
    ;;
  all)
    run_all_tests
    ;;
  *)
    echo -e "${RED}Usage: $0 [frontend|backend|all]${NC}"
    exit 1
    ;;
esac
