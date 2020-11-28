package com.nisovin.shopkeepers.config.value;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.nisovin.shopkeepers.util.Validate;

public class TypePatterns {

	public static TypePattern forClass(Class<?> clazz) {
		return new ClassTypePattern(clazz);
	}

	private static class ClassTypePattern implements TypePattern {

		private final Class<?> clazz;

		public ClassTypePattern(Class<?> clazz) {
			Validate.notNull(clazz, "clazz is null");
			this.clazz = clazz;
		}

		@Override
		public boolean matches(Type type) {
			if (this.clazz == type) return true;
			if (type instanceof ParameterizedType) {
				if (this.clazz == ((ParameterizedType) type).getRawType()) {
					return true;
				}
			}
			return false;
		}
	}

	public static TypePattern parameterized(Class<?> clazz, TypePattern... typeParameters) {
		return new ParameterizedTypePattern(clazz, typeParameters);
	}

	public static TypePattern parameterized(Class<?> clazz, Class<?>... typeParameters) {
		TypePattern[] typePatterns = null;
		if (typeParameters != null) {
			typePatterns = new TypePattern[typeParameters.length];
			for (int i = 0; i < typeParameters.length; ++i) {
				Class<?> typeParameter = typeParameters[i];
				Validate.notNull(typeParameter, "One of the typeParameters is null!");
				typePatterns[i] = TypePatterns.forClass(typeParameter);
			}
		}
		return parameterized(clazz, typePatterns);
	}

	private static class ParameterizedTypePattern extends ClassTypePattern {

		private final TypePattern[] typeParameters;

		public ParameterizedTypePattern(Class<?> clazz, TypePattern... typeParameters) {
			super(clazz);
			Validate.notNull(typeParameters, "typeParameters is null");
			Validate.notNull(typeParameters.length == 0, "typeParameters is empty");
			this.typeParameters = (typeParameters == null) ? null : typeParameters.clone();
		}

		@Override
		public boolean matches(Type type) {
			if (!super.matches(type)) return false;
			if (!(type instanceof ParameterizedType)) {
				return false;
			}
			Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
			if (typeArguments == null || typeArguments.length != this.typeParameters.length) {
				return false;
			}
			for (int i = 0; i < typeParameters.length; ++i) {
				if (!this.typeParameters[i].matches(typeArguments[i])) {
					return false;
				}
			}
			return true;
		}
	}

	public static TypePattern any() {
		return AnyTypePattern.INSTANCE;
	}

	/**
	 * Matches any type.
	 */
	private static class AnyTypePattern implements TypePattern {

		public static final AnyTypePattern INSTANCE = new AnyTypePattern();

		public AnyTypePattern() {
		}

		@Override
		public boolean matches(Type type) {
			return true;
		}
	}

	private TypePatterns() {
	}
}
