#!/usr/bin/env/ python
# -*- coding: utf-8 -*-

import re
from setuptools import setup, find_packages

requirements = []

with open("audit_middleware/__init__.py", "r") as f:
    version = re.search(
        r'^__version__\s*=\s*[\'"]([^\'"]*)[\'"]',  # type: ignore
        f.read(),
        re.MULTILINE,
    ).group(1)

setup(
    name="audit_middleware",
    version=version,
    author="University of Pennsylvania",
    author_email="pennsieve.support@gmail.com",
    description="A library for generating audit log events on the Pennsieve Platform (internal only)",
    packages=find_packages(),
    package_dir={"audit_middleware": "audit_middleware"},
    install_requires=requirements,
    license="",
    classifiers=["Development Status :: 2 - Alpha", "Topic :: Utilities"],
)
