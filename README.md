# Xmpp API library

The Xmpp API provides methods to execute Xmpp operations, such as send message, receive message, and more without user interaction from background threads. This is done by connecting your client application to a remote service provided by [Conversations](https://conversations.im/) or other Xmpp providers.

### License
While Conversations itself is GPLv3+, the API library is licensed under Apache License v2.
Thus, you are allowed to also use it in closed source applications as long as you respect the [Apache License v2](https://github.com/moparisthebest/xmpp-api/blob/master/LICENSE).

### Add the API library to your project

For development or before first version is released, you'll have to run `./gradlew publishToMavenLocal` then add this to your build.gradle:

```gradle
repositories {
    mavenLocal()
}

dependencies {
    compile 'com.moparisthebest:xmpp-api:1.0-SNAPSHOT'
}
```

After initial release add this to your build.gradle:

```gradle
repositories {
    jcenter()
}

dependencies {
    compile 'com.moparisthebest:xmpp-api:1.0'
}
```

### Full example
A full working example is available in the [example project](https://github.com/moparisthebest/xmpp-api/blob/master/example). The [``XmppApiActivity.java``](https://github.com/moparisthebest/xmpp-api/blob/master/example/src/main/java/org/openintents/xmpp/example/XmppApiActivity.java) contains most relevant sourcecode.

### API

[XmppServiceApi](https://github.com/moparisthebest/xmpp-api/blob/master/xmpp-api/src/main/java/org/openintents/xmpp/util/XmppServiceApi.java) contains all possible Intents and available extras for a provider to implement.  
[XmppPluginCallbackApi](https://github.com/moparisthebest/xmpp-api/blob/master/xmpp-api/src/main/java/org/openintents/xmpp/util/XmppPluginCallbackApi.java) contains all possible Intents and available extras for a plugin to implement. 
