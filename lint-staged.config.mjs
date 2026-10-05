import { execSync } from 'node:child_process';

const hasJava = () => {
  try {
    execSync('java -version', { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
};

const spotlessApply = () =>
  hasJava()
    ? './server/mvnw -q -f server/pom.xml spotless:apply'
    : 'echo "warning: java not found, skipping spotless (CI will enforce formatting)"';

export default {
  'client/**/*.{ts,html}': ['npm --prefix client exec -- eslint --fix', 'prettier --write'],
  'client/**/*.{scss,css,json}': 'prettier --write',
  '*.{md,yml,yaml,json}': 'prettier --write',
  'server/**/*.java': spotlessApply,
};
