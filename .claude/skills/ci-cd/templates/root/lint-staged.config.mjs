export default {
  'client/**/*.{ts,html}': ['npm --prefix client exec -- eslint --fix', 'prettier --write'],
  'client/**/*.{scss,css,json}': 'prettier --write',
  '*.{md,yml,yaml,json}': 'prettier --write',
  'server/**/*.java': () => './server/mvnw -q -f server/pom.xml spotless:apply',
};
