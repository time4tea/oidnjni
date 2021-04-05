check_defined = \
    $(strip $(foreach 1,$1, \
        $(call __check_defined,$1,$(strip $(value 2)))))
__check_defined = \
    $(if $(value $1),, \
      $(error Undefined $1$(if $2, ($2))))


$(call check_defined, JAVA_HOME, java home for jni include files)

OIDN_PATH=build

JNI_INCLUDE=$(JAVA_HOME)/include $(JAVA_HOME)/include/darwin
OIDN_INCLUDE=$(OIDN_PATH)/include
OIDN_LIB=$(OIDN_PATH)/lib

INCLUDE=$(JNI_INCLUDE) $(OIDN_INCLUDE)
LIB=$(OIDN_LIB)

SRCS=../oidnjni-linux/src/main/c/*.c

all: build/liboidnjni.dylib

print-%  : ; @echo $* = $($*)

build/liboidnjni.dylib: $(SRCS)
	$(CC) -shared -fpic -Werror -Wall $(LINK_FLAGS) $(foreach d, $(INCLUDE), -I$d) $(foreach d, $(LIB), -L$d) $(LIB)/libOpenImageDenoise.1.2.4.dylib $(SRCS) -o $@

.PHONY: clean
clean:
	rm -f build/lib*.so

.PHONY: distclean
distclean:
	rm -rf build