package com.tms;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Guards the modular-monolith package boundaries from the C4 view. */
@AnalyzeClasses(packages = "com.tms", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

  @ArchTest
  static final ArchRule crossCuttingDoesNotDependOnDomainModules =
      noClasses()
          .that()
          .resideInAPackage("com.tms.crosscutting..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.tms.scheduling..",
              "com.tms.trainer..",
              "com.tms.customer..",
              "com.tms.certification..",
              "com.tms.feedback..",
              "com.tms.reporting..",
              "com.tms.admin..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule modulesAreFreeOfCycles =
      slices().matching("com.tms.(*)..").should().beFreeOfCycles().allowEmptyShould(true);
}
