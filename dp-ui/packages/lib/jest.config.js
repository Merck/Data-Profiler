module.exports = {
  roots: ['src'],
  testMatch: [
    '**/__tests__/**/*.+(ts|tsx|js)',
    '**/?(*.)+(spec|test).+(ts|tsx|js)',
  ],
  testPathIgnorePatterns: [
    '/node_modules/',
    '<rootDir>/dist/assets',
    '<rootDir>/dist/__tests__/mocks',
    '<rootDir>/src/__tests__/mocks',
  ],
  coverageDirectory: 'coverage',
  transform: {
    '^.+\\.(ts|tsx)$': 'ts-jest',
  },
}