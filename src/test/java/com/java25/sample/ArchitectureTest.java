package com.java25.sample;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;
import static com.tngtech.archunit.library.GeneralCodingRules.OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
    packages = "com.java25.sample",
    importOptions = ImportOption.DoNotIncludeTests.class)
// ArchUnit discovers rules from static @ArchTest fields; JUnit instantiates the class.
@SuppressWarnings("PMD.InstantiableUtilityClass")
class ArchitectureTest {

  @ArchTest static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

  @ArchTest static final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

  @ArchTest static final ArchRule noStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

  @ArchTest static final ArchRule noGenericExceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

  @ArchTest static final ArchRule noJodaTime = NO_CLASSES_SHOULD_USE_JODATIME;

  @ArchTest static final ArchRule noOldDateTime = OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED;

  @ArchTest static final ArchRule noDeprecatedApi = DEPRECATED_API_SHOULD_NOT_BE_USED;

  @ArchTest
  static final ArchRule controllersDoNotUseRepositories =
      noClasses()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .dependOnClassesThat()
          .areAssignableTo(Repository.class);

  @ArchTest
  static final ArchRule controllersDoNotSeeEntities =
      noClasses()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .dependOnClassesThat()
          .areAnnotatedWith(Entity.class);

  @ArchTest
  static final ArchRule noForbiddenLibraries =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("lombok..", "org.joda..")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("org.springframework.lang.Nullable")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("org.springframework.lang.NonNull")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("org.springframework.web.client.RestTemplate");
}
