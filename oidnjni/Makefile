check_defined = \
    $(strip $(foreach 1,$1, \
        $(call __check_defined,$1,$(strip $(value 2)))))
__check_defined = \
    $(if $(value $1),, \
      $(error Undefined $1$(if $2, ($2))))


$(call check_defined, JAVA_HOME, java home for jni include files)

OIDN_VERSION=1.2.4

OIDN_NAME=oidn-$(OIDN_VERSION).x86_64.linux
OIDN_PATH=$(realpath build/$(OIDN_NAME))

JNI_INCLUDE=$(JAVA_HOME)/include $(JAVA_HOME)/include/linux
OIDN_INCLUDE=$(OIDN_PATH)/include
OIDN_LIB=$(OIDN_PATH)/lib

INCLUDE=$(JNI_INCLUDE) $(OIDN_INCLUDE)
LIB=$(OIDN_LIB)

SRCS=src/main/c/*.c

UNPACKED_LIB=build/$(OIDN_NAME)/lib/libOpenImageDenoise.so

all: build/liboidnjni.so

print-%  : ; @echo $* = $($*)

build/$(OIDN_NAME).tar.gz:
	mkdir -p $(dir $@)
	curl --silent --fail -L -o $@ https://github.com/OpenImageDenoise/oidn/releases/download/v$(OIDN_VERSION)/$(OIDN_NAME).tar.gz

$(UNPACKED_LIB):	build/$(OIDN_NAME).tar.gz
	tar -z -x -f $< --touch -C build

build/liboidnjni.so: $(UNPACKED_LIB) $(SRCS)
	$(CC) -shared -fpic -Wl,--no-as-needed $(foreach d, $(INCLUDE), -I$d) $(foreach d, $(LIB), -L$d) -lOpenImageDenoise $(SRCS) -o $@

.PHONY: clean
clean:
	rm -f build/lib*.so

.PHONY: distclean
distclean:
	rm -rf build