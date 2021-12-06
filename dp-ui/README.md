# UI

This project uses a lerna "monorepo" setup. The goal is to have loosely coupled "applications" that are wrapped by a parent react application. Each application has it's own dependencies, but they are wrapped by lerna.

There is a "parent-ui" package (which is based off of create-react-app). This package should be as lightweight as possible, and each "UI experiment" should be in it's own package.

# Setup

```
yarn install
yarn run bootstrap
```

# Local Development

```
yarn start
```

## Creating a new app

Due to the way that the parent-ui compiles/bundles, we have use a transpiler for all the packages that feed into the parent-ui. We have chosen the typescript compiler (tsc) to accomplish this task. The use of the typescript language is optional, you can write normal javascript and the typescript compiler handles it. 

1) From this directory, run `yarn run add-package your-package-name`. Please note, the directory will be `your-package-name`, but the actual npm package will be `@dp-ui/your-package-name`
2) Import your package and assign it a root route in `dp-ui/packages/parent/src/Router.js`
2) Add an entry to the sidebar at `dp-ui/packages/parent/src/Sidebar.js`

## Adding new dependencies to your package

Since we're using Lerna in a mono-repo with shared dependencies, we have to use a little more caution when adding new depdendencies. In general, each package is free to use their own dependencies and versions. However, we want to keep our bundle size down, so we should try to keep dependencies "as similar" as possible. If one dp-package is using dependency@1.0.0, you should not add dependency@1.0.1. 

Instead of using `yarn add` or `npm install --save`, we have to use the lerna wrapper by issuing `yarn run lerna add moment --scope @dp-ui/your-package-name`

## Developing your own app

Please see DPContext.md for more info.
