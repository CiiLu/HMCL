/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.util.versioning;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// A version range in the [Maven version range syntax], for example `[1.0.0,1.0.5)`.
///
/// A range set is a `|`-separated list of individual restrictions, and a version is contained in
/// the set if it is contained in at least one of them. Every restriction is either an interval
/// surrounded by brackets, or a bare version which is a shorthand for the soft requirement.
///
/// Unlike [VersionRange], this class supports exclusive bounds.
///
/// [Maven version range syntax]: https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
@NotNullByDefault
public final class MavenVersionRange {

    /// Parses a Maven version range.
    ///
    /// @param range the textual range, for example `[1.0.0,1.0.5)` or `1.0`
    /// @return the parsed range, never `null`
    /// @throws IllegalArgumentException if `range` is blank or malformed
    public static MavenVersionRange parse(String range) {
        if (range == null || range.isBlank())
            throw new IllegalArgumentException("Version range is empty");

        // The restrictions are split by `|` manually: `String.split` drops trailing empty
        // restrictions, which would silently accept ranges such as "[1.0,2.0]|".
        List<Restriction> restrictions = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= range.length(); i++) {
            if (i == range.length() || range.charAt(i) == '|') {
                restrictions.add(parseRestriction(range.substring(start, i).trim()));
                start = i + 1;
            }
        }

        return new MavenVersionRange(restrictions);
    }

    /// Parses a single restriction of a version range.
    ///
    /// @param text the textual restriction
    /// @return the parsed restriction
    /// @throws IllegalArgumentException if `text` is malformed or denotes an empty interval
    private static Restriction parseRestriction(String text) {
        if (text.isEmpty())
            throw new IllegalArgumentException("Version range contains an empty restriction");

        char first = text.charAt(0);
        if (first != '[' && first != '(') {
            // A bare version is a soft requirement: Maven recommends it to express "tested with".
            return new Restriction(VersionNumber.asVersion(text));
        }

        char last = text.charAt(text.length() - 1);
        if ((last != ']' && last != ')') || text.length() < 3) {
            throw new IllegalArgumentException("Version range is missing a closing bracket: " + text);
        }

        String[] bounds = text.substring(1, text.length() - 1).split(",", -1);
        if (bounds.length > 2) {
            throw new IllegalArgumentException("Version range contains too many bounds: " + text);
        }

        boolean lowerInclusive = first == '[';
        boolean upperInclusive = last == ']';

        @Nullable VersionNumber lower = bounds[0].isBlank() ? null : VersionNumber.asVersion(bounds[0].trim());
        @Nullable VersionNumber upper = bounds.length == 2 && !bounds[1].isBlank() ? VersionNumber.asVersion(bounds[1].trim()) : null;

        if (lower != null && upper != null && isEmptyInterval(lower, lowerInclusive, upper, upperInclusive)) {
            throw new IllegalArgumentException("Version range is empty: " + text);
        }

        return new Restriction(null, lower, lowerInclusive, upper, upperInclusive);
    }

    /// Tests whether an interval with both bounds contains no version at all.
    ///
    /// @param lower          the lower bound
    /// @param lowerInclusive whether the lower bound belongs to the interval
    /// @param upper          the upper bound
    /// @param upperInclusive whether the upper bound belongs to the interval
    /// @return `true` if the interval is empty
    private static boolean isEmptyInterval(VersionNumber lower, boolean lowerInclusive, VersionNumber upper, boolean upperInclusive) {
        int result = lower.compareTo(upper);
        if (result > 0)
            return true;

        // The bounds are equal: the interval contains that single version unless it excludes it on
        // both sides, that is "(" together with ")". Note that both "[" with ")" and "(" with "]"
        // still contain one version.
        return result == 0 && !lowerInclusive && !upperInclusive;
    }

    private final List<Restriction> restrictions;

    private MavenVersionRange(List<Restriction> restrictions) {
        this.restrictions = List.copyOf(restrictions);
    }

    /// Tests whether the given version is contained in this range.
    ///
    /// @param version the version to test, for example `1.0.4`
    /// @return `true` if at least one restriction contains the version
    public boolean contains(String version) {
        VersionNumber number = VersionNumber.asVersion(version);
        for (Restriction restriction : restrictions) {
            if (restriction.contains(number))
                return true;
        }
        return false;
    }

    /// Tests whether this range consists of exactly one bare version, which Maven treats as a soft
    /// requirement rather than a strict one.
    ///
    /// @return `true` if this range is a soft requirement
    public boolean isSoft() {
        return restrictions.size() == 1 && restrictions.get(0).softVersion != null;
    }

    /// Returns the single version a [soft][#isSoft()] range refers to.
    ///
    /// @return the version as written in the range, or `null` if this range is not soft
    public @Nullable String softVersion() {
        return isSoft() ? restrictions.get(0).softVersion.toString() : null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < restrictions.size(); i++) {
            if (i > 0)
                builder.append('|');
            builder.append(restrictions.get(i));
        }
        return builder.toString();
    }

    /// A single restriction of a version range: either an interval or a bare version.
    ///
    /// @param softVersion    the version of a bare soft requirement, or `null` if this restriction
    ///                       is an interval
    /// @param lower          the lower bound, or `null` if the restriction has no lower bound
    /// @param lowerInclusive whether `lower` belongs to the restriction
    /// @param upper          the upper bound, or `null` if the restriction has no upper bound
    /// @param upperInclusive whether `upper` belongs to the restriction
    private record Restriction(
            @Nullable VersionNumber softVersion,
            @Nullable VersionNumber lower,
            boolean lowerInclusive,
            @Nullable VersionNumber upper,
            boolean upperInclusive
    ) {
        /// Creates a soft requirement that only matches a single version.
        ///
        /// @param version the required version
        Restriction(VersionNumber version) {
            this(version, null, true, null, true);
        }

        /// Tests whether this restriction contains the given version.
        ///
        /// @param version the version to test
        /// @return `true` if the version satisfies this restriction
        boolean contains(VersionNumber version) {
            if (softVersion != null)
                return softVersion.compareTo(version) == 0;

            if (lower != null) {
                int result = lower.compareTo(version);
                if (result > 0 || result == 0 && !lowerInclusive)
                    return false;
            }
            if (upper != null) {
                int result = upper.compareTo(version);
                if (result < 0 || result == 0 && !upperInclusive)
                    return false;
            }
            return true;
        }

        @Override
        public String toString() {
            if (softVersion != null)
                return softVersion.toString();

            // An unbounded interval is rendered with the bracket matching its lower bound, so
            // "[3.0,)" is printed as "[3.0,]".
            boolean closed = upper != null ? upperInclusive : lowerInclusive;

            return (lowerInclusive ? "[" : "(")
                    + (lower != null ? lower : "")
                    + ","
                    + (upper != null ? upper : "")
                    + (closed ? "]" : ")");
        }
    }
}
