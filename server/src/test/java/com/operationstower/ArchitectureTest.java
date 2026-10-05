package com.operationstower;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.operationstower",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule domain_is_pure =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..api..",
              "..application..",
              "..infrastructure..",
              "org.springframework..",
              "jakarta.persistence..",
              "com.fasterxml..");

  @ArchTest
  static final ArchRule application_ignores_adapters =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api..", "..infrastructure..");

  @ArchTest
  static final ArchRule spring_security_stays_in_its_adapters =
      noClasses()
          .that()
          .resideOutsideOfPackages("..platform.security..", "..identity.infrastructure..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.springframework.security..");
}
