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
    private static boolean patched = false;

    public static void premain(String args, Instrumentation instrumentation) {
        // unexploitable (11.12.2021) logger
        System.out.println("[LOG4j FIX] Initializing log4j anti bomb.");
        instrumentation.addTransformer(new StrSubstitutorTransformer(), true);
    }

    static class StrSubstitutorTransformer implements ClassFileTransformer, Opcodes {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (className.equals("com/sun/jndi/ldap/Connection")) {
                System.out.println("WARNING! Something tried to initialize com.sun.jndi.ldap.Connection");
                System.out.println("Calling halt() for security reasons");
                Runtime.getRuntime().halt(1);
            }
            // endsWith - tolerate remapped packages
            if (className.endsWith("org/apache/logging/log4j/core/lookup/StrSubstitutor")) {
                ClassReader classReader = new ClassReader(classfileBuffer);
                ClassWriter classWriter = new ClassWriter(classReader, 0);
                classReader.accept(new StrSubstitutorVisitor(ASM9, classWriter), 0);
                if (patched) {
                    System.out.println("[LOG4J FIX] Replacing log4j substitutor prefix matcher to none.");
                } else {
                    System.out.println("[LOG4J FIX] FAIELD! Cannot replace prefix in StrSubstitutor. You can be vulnerable!");
                }
                return classWriter.toByteArray();
            } else {
                return classfileBuffer;
            }
        }

        private static class StrSubstitutorVisitor extends ClassVisitor {
            public StrSubstitutorVisitor(int api, ClassVisitor classVisitor) {
                super(api, classVisitor);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ("<clinit>".equals(name)) {
                    return new NoneMatcherPrefixVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions));
                } else {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }
        }

        private static class NoneMatcherPrefixVisitor extends MethodVisitor {
            public NoneMatcherPrefixVisitor(int api, MethodVisitor child) {
                super(api, child);
            }

            // It changes the argument of ``StrMatcher.stringMatcher(DEFAULT_ESCAPE + "{")`` (default escape is '$')
            // to empty string which results in the creation of a `none matcher`.
            // https://github.com/apache/logging-log4j2/blob/44569090f1cf1e92c711fb96dfd18cd7dccc72ea/log4j-core/src/main/java/org/apache/logging/log4j/core/lookup/StrSubstitutor.java#L151
            // https://github.com/apache/logging-log4j2/blob/fd711524b028aa5adece141ecc366d170c771a8b/log4j-core/src/main/java/org/apache/logging/log4j/core/lookup/StrMatcher.java#L207-L212
            @Override
            public void visitLdcInsn(Object value) {
                if ("${".equals(value)) {
                    patched = true;
                    super.visitLdcInsn("");
                } else {
                    super.visitLdcInsn(value);
                }
            }
        }
    }
}
