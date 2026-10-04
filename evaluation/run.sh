#!/bin/sh
set -eu
cd "$(dirname "$0")/../backend"
mvn -Dtest=ExtractionEvaluationIT test
