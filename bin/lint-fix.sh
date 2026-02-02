#!/bin/bash
# Script para executar todos os comandos de correção de lint
# Uso: ./bin/lint-fix.sh ou lein run-script lint-fix

set -e

echo "🔧 Executando correções automáticas de lint..."
echo ""

echo "📝 1/4 - Organizando namespaces (clojure-lsp clean-ns)..."
lein clojure-lsp clean-ns || true

echo "✨ 2/4 - Formatando código (clojure-lsp format)..."
lein clojure-lsp format || true

echo "🎨 3/4 - Formatando com cljfmt..."
lein cljfmt fix || true

echo "📦 4/4 - Organizando requires (nsorg)..."
lein nsorg --replace || true

echo ""
echo "✅ Todas as correções foram aplicadas!"
echo "💡 Execute 'lein lint' para verificar se ainda há problemas."
