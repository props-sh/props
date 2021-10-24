General:
- TemplateStringUtils / renderTemplate
- RefactoredProp
- define AbstractBaseProps
- prop builder or multi constructors
- read Resolver config from JVM arg or property file (see SourceUtils)

Async updates:
- Refactor AbstractPropGroup to override onValueUpdate (attempt to pass the epoch and ensure only the most recent value is sent)
- think of same Prop being bound to multiple Registries

Java:
- module-info.java and exports
- fix Gradle + git hook install
