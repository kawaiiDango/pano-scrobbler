%global _pkgname pano-scrobbler
%global _pkgver 439
%global _pkgdir /opt/%{_pkgname}

# Suppress debug package and stripping for prebuilt binaries
%global debug_package %{nil}
%global __strip /bin/true

Name:           pano-scrobbler
Version:        4.39
Release:        1%{?dist}
Summary:        Feature packed cross-platform music tracker
License:        GPL-3.0-or-later
URL:            https://github.com/kawaiiDango/pano-scrobbler
ExclusiveArch:  x86_64 aarch64

Source0:        %{url}/releases/download/%{_pkgver}/%{_pkgname}-linux-x64.tar.gz
Source1:        %{url}/releases/download/%{_pkgver}/%{_pkgname}-linux-arm64.tar.gz

Requires:       dbus
Requires:       webkitgtk6.0

Requires(post): %{_bindir}/update-alternatives
Requires(preun): %{_bindir}/update-alternatives

%description
Feature rich scrobbler for Windows, Linux & Android.
Supports Last.fm, ListenBrainz, Libre.fm & Pleroma.
With regex edits, charts & Discord Rich Presence on PC. 

%prep
# -c creates a wrapping directory since the tarball does not have one
%ifarch x86_64
%setup -q -c -T -a 0
%endif
%ifarch aarch64
%setup -q -c -T -a 1
%endif

# Patch desktop entry
sed -i 's|^Exec=.*|Exec=/usr/bin/%{_pkgname} %%U|' %{_pkgname}.desktop
sed -i 's|^Icon=.*|Icon=%{_pkgname}|' %{_pkgname}.desktop

%build
# Nothing to build — prebuilt binaries

%install
# Shared libs
install -d %{buildroot}%{_pkgdir}/lib
install -m644 *.so %{buildroot}%{_pkgdir}/
install -m644 lib/*.so %{buildroot}%{_pkgdir}/lib/

# Main executable
install -m755 %{_pkgname} %{buildroot}%{_pkgdir}/%{_pkgname}

# Desktop entry
install -Dm644 %{_pkgname}.desktop \
    %{buildroot}%{_datadir}/applications/%{_pkgname}.desktop

# Icon
install -Dm644 %{_pkgname}.svg \
    %{buildroot}%{_datadir}/icons/hicolor/scalable/apps/%{_pkgname}.svg

# License
install -Dm644 LICENSE \
    %{buildroot}%{_datadir}/licenses/%{_pkgname}/LICENSE

%post
update-alternatives --install %{_bindir}/%{_pkgname} %{_pkgname} \
    %{_pkgdir}/%{_pkgname} 100

%preun
if [ $1 -eq 0 ]; then
    update-alternatives --remove %{_pkgname} %{_pkgdir}/%{_pkgname}
fi

%files
%{_pkgdir}/
%ghost %{_bindir}/%{_pkgname}
%{_datadir}/applications/%{_pkgname}.desktop
%{_datadir}/icons/hicolor/scalable/apps/%{_pkgname}.svg
%{_datadir}/licenses/%{_pkgname}/LICENSE

%changelog
* Tue May 12 2026 kawaiiDango <kawaiiDango@protonmail.com> - 4.39-1
- Update to 4.39
