#!/usr/bin/env groovy

/*
 * Copyright 2024 Bernd Michaely (info@bernd-michaely.de).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Utility to create test zip files in the test resources

import groovy.cli.commons.CliBuilder
import groovy.transform.Field
import java.nio.file.*

@Field Class classPathFactory = this.class.classLoader.parseClass(new File('./PathFactory.java'))

@Field boolean dry_run
@Field boolean be_verbose
@Field boolean do_run

void runAction() throws IOException {
	final int depth = 3
	final int width = 3
	final int num = 3
	final Calendar c = Calendar.getInstance()
	final String today = String.format('%04d-%02d-%02d',
		c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
	final String userName = System.getProperty('user.name')
	final String tmpDir = System.getProperty('java.io.tmpdir')
	final Path root = Paths.get(tmpDir, userName, today)
	println "--> Writing to base dir : »${root}«"
	final Path[] sub_root = new Path[num + 1]
	for (int i = 1; i <= num; i++)
	{
		sub_root[i] = Paths.get(root.toString(), 'test' + i)
		final var pathFactory = classPathFactory.newInstance()
		pathFactory.setRootPath(sub_root[i].toString())
		final char baseChar = ('a' as char) + depth * i
		pathFactory.createPaths(depth, width, baseChar)
		final Path textPath = Paths.get(sub_root[i].toString(), String.valueOf(baseChar))
		final String fileName = "test${i}.txt"
		final Path textFile = Paths.get(textPath.toString(), fileName)
		if (dry_run || be_verbose) {
			pathFactory.logPaths()
			println "Writing text file »${textFile}« …"
		}
		if (!dry_run) {
				pathFactory.mkdirPaths()
				Files.write(textFile, List.of("Hello from ${fileName}."))
		}
	}
	final File cwd = root.toFile()
	String[] options = [ 'zip', '-r' ]
	if (!be_verbose) options += '-q'
	runCommand(cwd, options + [ "${root}/test2/g/h/i/test3.zip", 'test3' ])
	runCommand(cwd, options + [ "${root}/test1/d/e/f/test2.zip", 'test2' ])
	runCommand(cwd, options + [ "${root}/test1.zip", 'test1' ])
}

parse_command_line_options()

// === === === === ===
// ===  UTILITIES  ===
// === === === === ===

void parse_command_line_options() {
	def script_name = getClass().getSimpleName() + '.groovy'
	def script_version = '1.0'

	// parse command line options:
	def cli = new CliBuilder(
		usage : "$script_name [options]",
		header: 'Options:',
		footer: 'Create test.zip file for unit tests.'
	)
	cli.d(longOpt: 'dry-run', 'dry run (show only, what would be created)')
	cli.h(longOpt: 'help', 'print this message')
	cli.r(longOpt: 'run', 'create the test file')
	cli.v(longOpt: 'verbose', 'be verbose')
	cli.V(longOpt: 'version', 'show version and exit')
	def options = cli.parse(args)

	// evaluate command line options:

	if (!options) _exit (1, 'Error parsing command line options')

	dry_run = options.d
	be_verbose = options.v
	do_run = options.r

	if (options.h) { cli.usage() ; _exit() }
	if (options.V) {
		if (be_verbose)
			println "$script_name : $script_version"
		else
			println "$script_version"
		_exit()
	}

	final int n = options.arguments().size()

	if (n > 0) {
		cli.usage()
		_exit (2, 'Invalid command line options')
	}

	if (!dry_run && !do_run) {
		cli.usage()
		_exit (2, 'Invalid command line options')
	}

	// perform actions:
	runAction()
}

void runCommand(File cwd, String[] cmdArgs) {
	if (dry_run || be_verbose) {
		print 'Executing:'
		for (arg in cmdArgs) printf ("  »%s«%n", arg)
	}
	if (!dry_run) new ProcessBuilder(cmdArgs).directory(cwd).inheritIO().start().waitFor()
}

/** Exit the script successfully (that is returning 0). */
static void _exit() {
	System.exit 0
}

/** Exit the script with the given error code and message. */
static void _exit(int errorCode, Object message) {
	println message
	System.exit errorCode
}


