plugins {
    id("me.champeau.jmh")
    `maven-publish`
}

jmh {
    iterations.set(1)
}

