#!/usr/bin/env python

import sys
import os
import subprocess

from_commit = sys.argv[1]
to_commit = sys.argv[2]

deps = {}

def add_edges(from_dep, *to):
    for t in to:
        if not t in deps:
            deps[t] = []
        deps[t].append(from_dep)

def find_deps(module):
    if not module in deps:
        return [module]
    l = [module]
    for d in deps[module]:
        l.extend(find_deps(d))
    return l

def setup_deps():
    add_edges("runelite-client", "runelite-api", "runelite-jshell", "runelite-script-assembler-plugin")
    add_edges("runelite-script-assembler-plugin", "cache")
    add_edges("cache-client", "cache")
    add_edges("cache-updater", "cache-client")

def find_modified_modules():
    modifiedFiles = subprocess.check_output(['git', 'diff', from_commit, to_commit, '--name-only']).decode("utf-8")
    modifiedDirs = [x.split('/')[0] for x in modifiedFiles.split('\n')]
    modifiedDirs = filter(lambda d: len(d) > 0, modifiedDirs)
    modifiedDirs = set(modifiedDirs)
    return modifiedDirs

def find_tests(module):
    tests = []
    for root, dirs, files in os.walk(module + '/src/test/java'):
        for file in files:
            tests.append(os.path.join(root, file).replace(module + '/src/test/java/', ''))
    return tests

modified = find_modified_modules()
print("Modified modules:", modified)

setup_deps()
testable_modules = set()
for module in modified:
    testable_modules = testable_modules.union(find_deps(module))

print("Testable modules", testable_modules)
#print(modified)

numtests = 0
with open('tests.txt', 'w') as f:
    for module in testable_modules:
        f.write('# tests for ' + module + '\n')
        for test in find_tests(module):
            f.write(test + '\n')
            numtests = numtests + 1

print("Wrote", numtests, "tests")
