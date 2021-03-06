package restx.shell;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import restx.common.Mustaches;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: xavierhanin
 * Date: 4/9/13
 * Time: 10:24 PM
 */
public abstract class StdShellCommand implements ShellCommand {
    private final ImmutableList<String> aliases;
    private final String shortDescription;

    protected StdShellCommand(ImmutableList<String> aliases, String shortDescription) {
        this.aliases = Preconditions.checkNotNull(aliases);
        if (aliases.isEmpty()) {
            throw new IllegalArgumentException("aliases must not be empty");
        }

        this.shortDescription = shortDescription;
    }

    @Override
    public Optional<? extends ShellCommandRunner> match(String line) {
        for (String alias : aliases) {
            if (matchCommand(line, alias)) {
                return doMatch(line);
            }
        }

        return Optional.absent();
    }

    private boolean matchCommand(String line, String commandName) {
        return line.equals(commandName) || line.startsWith(commandName + " ");
    }

    protected abstract Optional<? extends ShellCommandRunner> doMatch(String line);

    @Override
    public void help(Appendable appendable) throws IOException {
        RestxShell.printIn(appendable, String.format("%10s", aliases.get(0)), RestxShell.AnsiCodes.ANSI_GREEN);
        appendable.append(" - " + getShortDescription() + "\n");
    }

    @Override
    public void man(Appendable appendable) throws IOException {
        RestxShell.printIn(appendable, aliases.get(0), RestxShell.AnsiCodes.ANSI_GREEN);
        appendable.append(" - " + getShortDescription() + "\n");
        if (aliases.size() > 1) {
            appendable.append("  aliases: " + Joiner.on(", ").join(aliases.subList(1, aliases.size())) + "\n");
        }
    }

    @Override
    public Iterable<Completer> getCompleters() {
        return Collections.<Completer>singleton(commandCompleter());
    }

    protected StringsCompleter commandCompleter() {
        return new StringsCompleter(aliases);
    }

    public ImmutableList<String> getAliases() {
        return aliases;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    protected <T> ImmutableMap<Mustache, String> buildTemplates(Class<T> clazz, ImmutableMap<String, String> tpls) {
        ImmutableMap.Builder<Mustache, String> builder = ImmutableMap.builder();

        for (Map.Entry<String, String> entry : tpls.entrySet()) {
            builder.put(Mustaches.compile(clazz, entry.getKey()), entry.getValue());
        }

        return builder.build();
    }

    protected void generate(ImmutableMap<Mustache, String> templates, Path path, Object scope) throws IOException {
        for (Map.Entry<Mustache, String> entry : templates.entrySet()) {
            Mustaches.execute(entry.getKey(), scope, resolvePath(path, entry.getValue(), scope));
        }

    }

    private Path resolvePath(Path path, String relative, Object scope) {
        return path.resolve(Mustaches.execute(
                new DefaultMustacheFactory().compile(new StringReader(relative), relative), scope));
    }

    protected List<String> splitArgs(String line) {
        return RestxShell.splitArgs(line);
    }
}
