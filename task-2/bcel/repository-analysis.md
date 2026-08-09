##======================================================
``bash 
DEFAULT_BRANCH=master
COMMIT=$(git rev-list -1 --before="2026-01-01 00:00:00 +0000" "origin/$DEFAULT_BRANCH")
git checkout --detach "$COMMIT"
git log -1 --format="Commit: %H%nDate: %cI%nAuthor: %an%nSubject: %s"
``

Repository: commons-bcel
Default branch: master
Commit hash: 2e51e5e0b660d328802a5ace8aaa2f94f6b5aa80
Commit date: 2025-12-31T18:33:40-05:00
Author: Gary Gregory
Subject: Javadoc: The @deprecated tag should be last.

##==============================================================

Build system: Maven
Build command: mvn clean verify -DskipTests
Build status: Succeeded
Tests: Skipped (-DskipTests flag)

##====================================================
#=================
BCEL ka analysis: basePlugin remove + requiredForLanguages add karne ke baad BUILD SUCCESS
Analysis time: 51.432s
Task ID: 22aa96ee-dabc-422f-84bd-8cb7194d09bf

===========================
Fields whose null-checks are duplicated (ExcessiveNullCheck)=7

Classes should not dispatch behavior (TypeChecking)=6

S1067 (Complicated Boolean)=13

S2301 (Flag Argument)=7


