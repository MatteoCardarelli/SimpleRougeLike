# Regole di R8 per l'APK dei test strumentati, solo quando i test girano sulla build con R8.
#
# I testFixtures di :domain contengono anche i test di contratto delle porte, scritti con JUnit 5 e
# AssertJ. Sul dispositivo quelle librerie sono escluse, perché servono solo classi come
# EventLogWriter: i test di contratto non girano mai lì, quindi i riferimenti mancanti si ignorano.
-dontwarn org.junit.jupiter.**
-dontwarn org.junit.platform.**
-dontwarn org.assertj.**
-dontwarn net.jqwik.**
