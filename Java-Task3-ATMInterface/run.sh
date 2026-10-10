#!/usr/bin/env bash
# Compile and run the ATM Interface (requires JDK 17+)
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/com/oasis/atm/*.java && java -cp out com.oasis.atm.Main
