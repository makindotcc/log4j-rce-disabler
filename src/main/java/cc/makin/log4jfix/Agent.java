package cc.makin.log4jfix;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Agent {
    public static void premain(String args, Instrumentation instrumentation) {
        // unexploitable (11.12.2021) logger
        System.out.println("[LOG4j FIX] Initializing log4j anti bomb.");
        instrumentation.addTransformer(new StrSubstitutorTransformer(), true);
    }

    static class StrSubstitutorTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            // endsWith - tolerate remapped packages
            if (className.endsWith("org/apache/logging/log4j/core/lookup/StrSubstitutor")) {
                ClassReader classReader = new ClassReader(classfileBuffer);
                ClassWriter classWriter = new ClassWriter(classReader, 0);
                classReader.accept(new StrSubstitutorOdwiedzacz(classWriter), 0);
                return classWriter.toByteArray();
            } else {
                return classfileBuffer;
            }
        }

        private static class StrSubstitutorOdwiedzacz extends ClassVisitor {
            public StrSubstitutorOdwiedzacz(ClassVisitor classVisitor) {
                super(Opcodes.ASM8, classVisitor);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ("<clinit>".equals(name)) {
                    return new NoneMatcherPrefixVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
                } else {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }
        }

        private static class NoneMatcherPrefixVisitor extends MethodVisitor {
            public NoneMatcherPrefixVisitor(MethodVisitor child) {
                super(Opcodes.ASM8, child);
            }

            @Override
            public void visitLdcInsn(Object value) {
                if ("${".equals(value)) {
                    System.out.println("[LOG4J FIX] Replacing log4j substitutor prefix matcher to none.");
                    super.visitLdcInsn("");
                } else {
                    super.visitLdcInsn(value);
                }
            }
        }
    }
}
