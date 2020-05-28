package com.apt.todos

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchTest {

    @Test
    fun servicesAndRepositoriesShouldNotDependOnWebLayer() {

        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.apt.todos")

        noClasses()
            .that()
                .resideInAnyPackage("com.apt.todos.service..")
            .or()
                .resideInAnyPackage("com.apt.todos.repository..")
            .should().dependOnClassesThat()
                .resideInAnyPackage("..com.apt.todos.web..")
        .because("Services and repositories should not depend on web layer")
        .check(importedClasses)
    }
}
