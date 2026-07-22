package dev.helikon.client.integration.network;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small SemVer-compatible comparator for local and GitHub release versions. */
public final class ReleaseVersion implements Comparable<ReleaseVersion> {
    private static final Pattern VERSION = Pattern.compile(
            "^[vV]?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;

    private ReleaseVersion(int major, int minor, int patch, String preRelease) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
    }

    public static ReleaseVersion parse(String value) {
        Objects.requireNonNull(value, "value");
        Matcher matcher = VERSION.matcher(value.strip());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid release version: " + value);
        }
        String preRelease = matcher.group(4);
        if (preRelease != null && java.util.Arrays.stream(preRelease.split("\\.", -1)).anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("Invalid release version: " + value);
        }
        try {
            return new ReleaseVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), preRelease);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Release version component is too large: " + value, exception);
        }
    }

    @Override
    public int compareTo(ReleaseVersion other) {
        Objects.requireNonNull(other, "other");
        int compared = Integer.compare(major, other.major);
        if (compared == 0) {
            compared = Integer.compare(minor, other.minor);
        }
        if (compared == 0) {
            compared = Integer.compare(patch, other.patch);
        }
        if (compared != 0) {
            return compared;
        }
        if (preRelease == null || other.preRelease == null) {
            return preRelease == null ? other.preRelease == null ? 0 : 1 : -1;
        }
        return compareIdentifiers(preRelease, other.preRelease);
    }

    private static int compareIdentifiers(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        for (int index = 0; index < Math.min(leftParts.length, rightParts.length); index++) {
            String leftPart = leftParts[index];
            String rightPart = rightParts[index];
            if (leftPart.equals(rightPart)) {
                continue;
            }
            boolean leftNumeric = leftPart.chars().allMatch(Character::isDigit);
            boolean rightNumeric = rightPart.chars().allMatch(Character::isDigit);
            if (leftNumeric && rightNumeric) {
                return compareNumericIdentifiers(leftPart, rightPart);
            }
            if (leftNumeric != rightNumeric) {
                return leftNumeric ? -1 : 1;
            }
            return leftPart.compareTo(rightPart);
        }
        return Integer.compare(leftParts.length, rightParts.length);
    }

    private static int compareNumericIdentifiers(String left, String right) {
        String normalizedLeft = left.replaceFirst("^0+(?!$)", "");
        String normalizedRight = right.replaceFirst("^0+(?!$)", "");
        int length = Integer.compare(normalizedLeft.length(), normalizedRight.length());
        return length != 0 ? length : normalizedLeft.compareTo(normalizedRight);
    }
}
