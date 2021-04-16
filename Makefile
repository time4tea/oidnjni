check_defined = \
    $(strip $(foreach 1,$1, \
        $(call __check_defined,$1,$(strip $(value 2)))))
__check_defined = \
    $(if $(value $1),, \
      $(error Undefined $1$(if $2, ($2))))

$(call check_defined, JAVA_HOME, java home for jni include files)

space :=
space +=
join-with = $(subst $(space),$(1),$(strip $(2)))
to-classpath = $(call join-with,:,$(1))

OS!=uname

OIDN_VERSION=1.2.4
KOTLIN_VERSION=1.4.32

DOWNLOAD=download
TARGET=out

DISTRIBUTION=$(TARGET)/distribution

OIDN_SITE=https://github.com/OpenImageDenoise/oidn/releases/download
ARCH=x86_64
LINUX=$(ARCH).linux
MACOS=$(ARCH).macos

OIDNJNI_VERSION=DEVELOPER

CURL=curl -L --fail --silent
JAR=$(JAVA_HOME)/bin/jar
JAVA=$(JAVA_HOME)/bin/java
ANT=$(realpath apache-ant-1.10.9)/bin/ant

DOWNLOAD_OIDN=$(DOWNLOAD)/oidn
DOWNLOAD_KOTLIN=$(DOWNLOAD)/kotlinx

OIDN_LINUX=oidn-$(OIDN_VERSION).$(LINUX)
OIDN_MACOS=oidn-$(OIDN_VERSION).$(MACOS)

KOTLIN_SITE=https://github.com/JetBrains/kotlin/releases/download
KOTLIN_DIR=$(DOWNLOAD_KOTLIN)/kotlin-$(KOTLIN_VERSION)
KOTLINC=$(KOTLIN_DIR)/kotlinc/bin/kotlinc

JNI_SRCS=src/main/oidnjni-native/*.c

all: oidn_native oidnlib_kotlin jni

ifeq ($(OS),Linux)
include Makefile.linux
else ifeq ($(OS),Darwin)
include Makefile.macos
else
$(error Unsupported platform '$(OS)')
endif

oidnjni-native: $(DISTRIBUTION)/oidnjni-linux-$(OIDNJNI_VERSION).jar

$(DISTRIBUTION)/oidnjni-%-$(OIDNJNI_VERSION).jar: jni


.PHONY: oidnlib_kotlin
oidnlib_kotlin: $(DISTRIBUTION)/oidnlib-kotlin-$(OIDN_VERSION).jar $(DISTRIBUTION)/oidnjni-kotlin-$(OIDNJNI_VERSION).jar $(DISTRIBUTION)/oidnjni-kotlin-$(OIDNJNI_VERSION)-test.jar

KOTLIN_LIBS=$(shell find lib/kotlin -name *.jar)
TEST_LIBS=$(shell find lib/test -name *.jar)

$(DISTRIBUTION)/oidnlib-kotlin-$(OIDN_VERSION).jar: $(KOTLINC) $(shell find src/main/oidnlib-kotlin)
	$(KOTLINC) -d $@ -classpath $(call to-classpath,$(KOTLIN_LIBS)) src/main/oidnlib-kotlin

$(DISTRIBUTION)/oidnjni-kotlin-$(OIDNJNI_VERSION).jar: $(DISTRIBUTION)/oidnlib-kotlin-$(OIDN_VERSION).jar $(KOTLINC) $(shell find src/main/oidn-lib)
	$(KOTLINC) -d $@ -classpath $(call to-classpath,$(filter %.jar,$^) $(KOTLIN_LIBS)) src/main/oidn-lib

$(DISTRIBUTION)/oidnjni-kotlin-$(OIDNJNI_VERSION)-test.jar: $(DISTRIBUTION)/oidnjni-kotlin-$(OIDNJNI_VERSION).jar $(DISTRIBUTION)/oidnlib-kotlin-$(OIDN_VERSION).jar $(KOTLINC) $(shell find src/test/oidn-lib) $(shell find src/test/resources)
	$(KOTLINC) -d $@ -classpath $(call to-classpath,$(filter %.jar,$^) $(KOTLIN_LIBS) $(TEST_LIBS)) src/test/oidn-lib
	$(JAR) uf $@ -C src/test/resources .

.PHONY: oidn_native
oidn_native: $(DISTRIBUTION)/oidn-native-linux-$(OIDN_VERSION).jar $(DISTRIBUTION)/oidn-native-macos-$(OIDN_VERSION).jar

$(DISTRIBUTION)/oidn-native-linux-$(OIDN_VERSION).jar: $(DOWNLOAD_OIDN)/oidn-$(OIDN_VERSION).$(LINUX)/.uptodate
	@mkdir -p $(dir $@)
	$(JAR) cf $@ -C $(DOWNLOAD_OIDN)/$(OIDN_LINUX) lib

$(DOWNLOAD_OIDN)/$(OIDN_LINUX)/.uptodate: $(DOWNLOAD_OIDN)/oidn-$(OIDN_VERSION).$(LINUX).tar.gz
	$(TAR) -x -C $(DOWNLOAD_OIDN) -f $(realpath $<)
	touch $@

$(DOWNLOAD_OIDN)/$(OIDN_LINUX).tar.gz:
	@mkdir -p $(dir $@)
	$(CURL) $(OIDN_SITE)/v$(OIDN_VERSION)/$(notdir $@) -o $@

$(DISTRIBUTION)/oidn-native-macos-$(OIDN_VERSION).jar: $(DOWNLOAD_OIDN)/oidn-$(OIDN_VERSION).$(MACOS)/.uptodate
	@mkdir -p $(dir $@)
	$(JAR) cf $@ -C $(DOWNLOAD_OIDN)/$(OIDN_MACOS) lib

$(DOWNLOAD_OIDN)/$(OIDN_MACOS)/.uptodate: $(DOWNLOAD_OIDN)/oidn-$(OIDN_VERSION).$(MACOS).tar.gz
	$(TAR) -x -C $(DOWNLOAD_OIDN) -f $(realpath $<)
	touch $@

$(DOWNLOAD_OIDN)/$(OIDN_MACOS).tar.gz:
	@mkdir -p $(dir $@)
	$(CURL) $(OIDN_SITE)/v$(OIDN_VERSION)/$(notdir $@) -o $@

$(KOTLINC): $(KOTLIN_DIR)/.uptodate

$(KOTLIN_DIR)/.uptodate: $(DOWNLOAD_KOTLIN)/kotlin-compiler-$(KOTLIN_VERSION).zip
	@mkdir -p $(dir $@)
	(cd $(KOTLIN_DIR) && unzip -q $(realpath $<))
	touch $@

$(DOWNLOAD_KOTLIN)/kotlin-compiler-$(KOTLIN_VERSION).zip:
	@mkdir -p $(dir $@)
	$(CURL) $(KOTLIN_SITE)/v$(KOTLIN_VERSION)/$(notdir $@) -o $@

ifndef DISPLAY
JAVA_OPTS=-Djava.awt.headless=true
endif

.PHONY: setup
setup:
	$(ANT)

.PHONY: test
test: all
	@echo Running tests...
	@$(JAVA) $(JAVA_OPTS) -classpath $(call to-classpath,$(filter %.jar,$(shell find $(DISTRIBUTION)) $(shell find lib/kotlin) $(shell find lib/test))) org.junit.platform.console.ConsoleLauncher --scan-classpath $(call to-classpath,$(filter %-test.jar, $(shell find $(DISTRIBUTION))))


.PHONY: clean
clean:
	rm -rf $(TARGET)

.PHONY: distclean
distclean: clean
	rm -rf $(DOWNLOAD) lib


print-%  : ; @echo $* = $($*)
