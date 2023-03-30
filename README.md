# Themis
![Java CI with Maven](https://github.com/kpouer/WKTParser/workflows/Java%20CI%20with%20Maven/badge.svg)
![CodeQL](https://github.com/kpouer/WKTParser/workflows/CodeQL/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.kpouer/themis)](https://central.sonatype.com/artifact/com.kpouer/wktparser/1.1.1/versions)

## Introduction

It is a lightweight inversion of control container framework written in Java.
It is freely inspired by the Spring framework in a much smaller and simpler version.
Written for fun and learning purposes but might be used in any project you want.

## Dependency

Available through Maven central

```xml
<dependency>
  <groupId>com.kpouer</groupId>
  <artifactId>themis</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Features

It comes with two annotations : `@Component` and `@Qualifier`.
The first one is intended to be added on classes and methods that can be instantiated by the container.

Components are instantiated as singleton and lazily by default but you can change that with the arguments of the 
`@Component` annotation.

`@Qualifier` annotation is used on method parameters to help the container to choose the good Component in the case
multiple choices are possible.

## Usage

### 1. Create a new instance of the container

The constructor takes a list of packages to scan for components.

```java
Themis themis = new ThemisImpl("com.kpouer");
MainPanel painPanel = themis.getComponentOfType(MainPanel.class);
```

