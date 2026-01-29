# Contributing to gspatial

Thank you for your interest in contributing to gspatial!

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](../../issues)
2. If not, create a new issue with:
   - A clear, descriptive title
   - Steps to reproduce the bug
   - Expected vs actual behavior
   - Your environment (Neo4j version, Java version, OS)

### Suggesting Features

1. Open a new issue with the `enhancement` label
2. Describe the feature and its use case
3. Explain why this would be useful to other users

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`./gradlew test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Code Style

- Follow existing code conventions
- Add Javadoc comments for public methods
- Write unit tests for new features
- Keep commits atomic and well-described

### Development Setup

1. Clone the repository
2. Install Java 21 and Gradle
3. Build: `./gradlew build`
4. Run tests: `./gradlew test`

For integration tests that require Neo4j:
1. Copy `.env.example` to `.env`
2. Fill in your Neo4j credentials
3. Run: `./gradlew test`

## License

By contributing, you agree that your contributions will be licensed under the GPLv3 License.
